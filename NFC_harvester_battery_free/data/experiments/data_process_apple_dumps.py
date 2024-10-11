"""
Script to convert an Apple healthkit export into a protocol that can be used to label Densor data.
"""

import argparse
import experiment_master
import os
import re
import sys
import xml.etree.ElementTree as ET

from datetime import datetime

# Include plot_readings to load the corresponding densor binary file. 
sys.path.append('../')
import plot_readings

apple_dt_fmt = '%Y-%m-%d %H:%M:%S '
"""Date-time format used in the healthkit export file."""
apple_label_lookup = {
    'HKCategoryValueSleepAnalysisAwake' : 'Awake',
    'HKCategoryValueSleepAnalysisAsleepCore' : 'Core',
    'HKCategoryValueSleepAnalysisAsleepDeep' : 'Deep',
    'HKCategoryValueSleepAnalysisAsleepREM' : 'REM',
    'HKCategoryValueSleepAnalysisInBed' : 'Asleep'
}
"""Maps Apple sleep state identifiers into densor labels."""
pid_re = '[p][0-9]+'
"""Regular expression used to obtain the identification number of the test subject from the binary file path."""

def load_xml_file(xml_path):
    """
    Loads a given apple health export file and retrieves the root element of the xml tree.
    
    Parameters
    ----------
    xml_path : str
        Path to the Apple healthkit export file (XML).
    
    Returns
    -------
    root : Element
        The root of the xml tree in the file.
    """
    root = ET.parse(xml_path).getroot()
    return root

def load_bin_data(bin_path):
    """
    Loads a densor binary file and extracts its start and end time.

    Parameters
    ----------
    bin_path : str
        Path to the densor binary file.
    
    Returns
    -------
    start_epoch : int
        Start time of the densor binary file in seconds epoch.
    end_epoch : int
        End time fo the densor binary file in seconds epoch.
    """
    _, sensor_states, start_time, rtc_interval, startup_delay, memory_pointer, _ = plot_readings.read_data(bin_path)

    block_size = plot_readings.calculate_block_size(sensor_states)
    data_len_sec = ((memory_pointer - plot_readings.data_start_addr) // block_size) * rtc_interval
    start_epoch = start_time + (startup_delay * 60)
    end_epoch = start_epoch + data_len_sec

    return start_epoch, end_epoch

def filter_sleep_analysis_data(root, bin_start_epoch, bin_end_epoch, timediff):
    """
    Extracts all sleep analysis records that either end or start in the run-time window of the densor binary file.

    Parameters
    ----------
    root : Element
        The xml root containing all apple health records.
    bin_start_epoch : int
        The start time of the densor binary file in seconds epoch (start of the protocol).
    bin_end_epoch : int
        The end time of the densor binary file in seconds epoch (end of the protocol).
    timediff : int 
        Time difference in seconds between the system used to extract the binary file and the apple watch/iphone. Positive timediff means the apple watch/iphone is behind.

    Returns
    -------
    entries : list of Element
        List of all sleep analysis entries within the protocol window.
    """
    entries = []
    for entry in root.findall('Record'):
        if entry.attrib['type'] == 'HKCategoryTypeIdentifierSleepAnalysis':
            entry_start_str = entry.attrib['startDate'][:-5]
            entry_start = datetime.strptime(entry_start_str, apple_dt_fmt).timestamp() + timediff
            entry_end_str = entry.attrib['endDate'][:-5]
            entry_end = datetime.strptime(entry_end_str, apple_dt_fmt).timestamp() + timediff

            if (entry_start < bin_end_epoch and entry_end > bin_start_epoch):
                entries.append(entry)

    return entries

def retrieve_sleep_state_data(entries):
    """
    Filter sleep analysis entries on sleep states (Awake, Core, REM, Deep)

    Parameters
    ----------
    entries : list of Element
        List of entries to be filtered.

    Returns
    -------
    out_entries : list of Element
        Filtered list of entries. Contains Awake, Core, REM and Deep states.
    str
        "sleep state analysis", description of the filtered entries list produced.
    """
    out_entries = []
    for entry in entries:
        if entry.attrib['type'] == 'HKCategoryTypeIdentifierSleepAnalysis':
            if entry.attrib['value'] != 'HKCategoryValueSleepAnalysisInBed':
                out_entries.append(entry)

    return out_entries, "sleep state analysis"

def retrieve_sleep_awake_data(entries):
    """
    Filter sleep analysis entries on just awake and asleep states.

    Parameters
    ----------
    entries : list of Element
        List of entries to be filtered.

    Returns
    -------
    out_entries : list of Element
        Filtered list of entries. Contains Awake and Asleep states.
    str
        "sleep or awake analysis", description of the filtered entries list produced.
    """
    out_entries = []
    for entry in entries:
        if entry.attrib['type'] == 'HKCategoryTypeIdentifierSleepAnalysis':
            entry_value_str = entry.attrib['value']
            if entry_value_str == 'HKCategoryValueSleepAnalysisInBed' or entry_value_str == 'HKCategoryValueSleepAnalysisAwake':
                out_entries.append(entry)

    return out_entries[:-1], "sleep or awake analysis"

def entries_to_protocol(entries, bin_start_epoch, bin_end_epoch, timediff):
    """
    Converts a list of entries into a protocol that can be used to label Densor data.

    Parameters
    ----------
    entries : list of Element
        Filtered list of sleep analysis records. Does either contain Awake and Asleep states, or full sleep states (Awake, Core, REM and Deep)
    bin_start_epoch : int
        The start time of the densor binary file in seconds epoch (start of the protocol).
    bin_end_epoch : int
        The end time of the densor binary file in seconds epoch (end of the protocol).
    timediff : int 
        Time difference in seconds between the system used to extract the binary file and the apple watch/iphone. Positive timediff means the apple watch/iphone is behind.

    Returns
    -------
    protocol : list of tuple of str and int
        The protocol containing tuples of sleep states and their duration in seconds.
    """
    protocol = []
    entries_start = datetime.strptime(entries[0].attrib['startDate'][:-5], apple_dt_fmt).timestamp() + timediff
    entries_end = datetime.strptime(entries[-1].attrib['endDate'][:-5], apple_dt_fmt).timestamp() + timediff
    
    # If the start time of the binary is earlier then the start time of the healthkit data, it means that the user was not asleep yet. Append awake until start of HK data.
    if (bin_start_epoch < entries_start):
        protocol.append([apple_label_lookup['HKCategoryValueSleepAnalysisAwake'], int(entries_start - bin_start_epoch)])

    # Get start and end time of every entry, calculate duration in seconds and append it to the protocol.
    for entry in entries:
        entry_start_str = entry.attrib['startDate'][:-5]
        entry_start = datetime.strptime(entry_start_str, apple_dt_fmt).timestamp() + timediff
        entry_end_str = entry.attrib['endDate'][:-5]
        entry_end = datetime.strptime(entry_end_str, apple_dt_fmt).timestamp() + timediff
        entry_value_str = entry.attrib['value']

        out_value = apple_label_lookup[entry_value_str]
        entry_duration = int(min(entry_end, bin_end_epoch) - max(entry_start, bin_start_epoch))
        print(f"Start: {datetime.fromtimestamp(entry_start).strftime(apple_dt_fmt)} | End: {datetime.fromtimestamp(entry_end).strftime(apple_dt_fmt)} | Value: {entry_value_str:<39} | Out Label: {out_value:<5} | Duration: {entry_duration}")
        protocol.append([out_value, entry_duration])

    # If the end time of the binary is later then the end time of the health kit data, the user woke up before the densor stopped. Append awake from end last entry till densor end.
    if (bin_end_epoch > entries_end):
        protocol.append([apple_label_lookup['HKCategoryValueSleepAnalysisAwake'], int(bin_end_epoch - entries_end)])

    return protocol

def save_experiment(bin_path, desc, protocol, start_time):
    """
    Saves the protocol as an experiment to the index file.

    Parameters
    ----------
    bin_path : str
        Path to the Densor binary file corresponding to the protocol.
    desc : str
        Description of the protocol (if the protocol contains just awake/asleep or all sleep stages seperately)
    protocol : list of tuple of str and int
        The protocol containing tuples of sleep states and their duration in seconds.
    start_time : int
        Start time of the protocol in seconds epoch. Uses the start time as described in the binary file as a possible time difference is fixed while creating the protocol.
    """
    experiment = {}
    experiment['pid'] = int(re.search(pid_re, bin_path).group()[1:])
    experiment['description'] = desc
    experiment['protocol'] = protocol
    experiment['filename'] = os.path.basename(bin_path)
    experiment['datetime'] = datetime.fromtimestamp(start_time).strftime(experiment_master.complete_time_fmt)
    experiment['notes'] = "protocol taken from apple health export"

    experiment_master.save_index_file(experiment)


if __name__ == '__main__':
    """
    Main entry point of the script. Handles arguments and then creates the protocol and saves it.
    """
    retrieve_data = retrieve_sleep_state_data
    xml_path = ''
    bin_path = ''
    timediff = 0

    usage = "usage: python data_process_apple_dumps.py [options] [-x, --xml] \"path-to-xml\" [(-b, --bin)] \"path-to-bin\"\n\
        \toptions:\n\
        \t\t-a, --awake\t\tRetrieve protocol either awake or asleep\n\
        \t\t-s, --state\t\tRetrieve protocol of full sleep state (default)\n\
        \t\t-t, --timediff <int>\tAdd the given integer to the timestamp of the apple watch data, in hours\n"

    parser = argparse.ArgumentParser(description="Script that converts apple heathkit exports into a protocol that can be used to label densor data.")
    parser.add_argument('--timediff', '-t', type=int, nargs=1,
                    help='Add the given integer to the timestamp of the apple watch data, in hours.')
    run_mode = parser.add_mutually_exclusive_group()
    run_mode.add_argument('--state', '-s', action='store_true',
                    help='Retrieve protocol of full sleep state (default).')
    run_mode.add_argument('--awake', '-a', action='store_true',
                    help='Retrieve protocol of either awake or asleep.')
    parser.add_argument('file1', type=str, nargs=1,
                    help='Either the healthkit xml file or the densor binary file for which the protocol should be created.')
    parser.add_argument('file2', type=str, nargs='?',
                    help='The opposite of file1, either the healthkit xml file or the densor binary file for which the protocol should be created. If no binary file is supplied will run in test mode on a hard coded file.')
    
    args = parser.parse_args()

    if args.state:
        print("State")
        retrieve_data = retrieve_sleep_state_data
    elif args.awake:
        print("Awake")
        retrieve_data = retrieve_sleep_awake_data

    for f in [args.file1[0], args.file2]:
        if (f != None) and os.path.exists(f):
            _, ext = os.path.splitext(f)
            if ext.lower() == '.bin':
                bin_path = f
            elif ext.lower() == '.xml':
                xml_path = f
            else:
                print(f"Invalid file {f}. Only accepts .bin or .xml files.")
                exit(1)

    if args.timediff != None:
        timediff = args.timediff * 3600


    bin_start_epoch = 0
    bin_end_epoch = 0

    if xml_path == '':
        print("Please provide an xml file")
        exit(1)

    if bin_path == '':
        bin_start_epoch = 1710989555
        bin_end_epoch = 1711011155
        bin_path = '2024-03-21-p0-e-1.bin'
        if (input("Are you sure you want to run test mode (not providing a binary file?) (Y/n) ") != 'Y'):
            print("Exiting. Please provide a binary file!")
            exit(1)
    else:
        bin_start_epoch, bin_end_epoch = load_bin_data(bin_path)


    root = load_xml_file(xml_path)

    entries = filter_sleep_analysis_data(root, bin_start_epoch, bin_end_epoch, timediff)
    entries, desc = retrieve_data(entries)
    protocol = entries_to_protocol(entries, bin_start_epoch, bin_end_epoch, timediff)

    save_experiment(bin_path, desc, protocol, bin_start_epoch)
     