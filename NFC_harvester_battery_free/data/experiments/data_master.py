"""
Script used to label Densor and ear-mounted accelerometer data.
"""
import argparse
import csv
import experiment_master
import glob
import json
import sys

from datetime import datetime
from pathlib import Path

sys.path.append('../')
import plot_readings

index = {}
"""
Global to load the index file into so it can be modified before saving it back.
"""
sensor_labels = [["m_temp"], ["m_pd"], ["m_touch_0", "m_touch_1", "m_touch_2", "m_touch_3", "m_touch_4"], ["m_baro"], ["m_accel_x", "m_accel_y", "m_accel_z"], ["m_vdda"]]
"""
List of column labels that the sensor readings should get in the csv file.
"""
compensate = True
"""
If voltage compensation should be applied.
"""

def save_settings_index(bin_file_name, rc_enabeled, sensor_states, start_time, rtc_interval, startup_delay, memory_pointer):
    """
    Takes all densor settings registers from a given binary file and saves it in the index file. Also saves previously made changes to the global index variable.
    
    Parameters
    ----------
    bin_file_name : str
        Name of the binary file corresponding to these settings.
    rc_enabled : bool
        True if the RTC used RC mode, False if the RTC used XT mode.
    sensor_states : list of bool
        List of booleans, describing which sensors where turned on. Order: Temperature, Photodiode, Touch, Barometer, Accelerometer.
    start_time : int
        Second epoch when the Densor was started. Time of the first reading.
    rtc_interval : int
        Interval between readings in seconds.
    startup_delay : int
        Delay before the first reading in minutes.
    memory_pointer : int
        Pointer to the first memory address that does not contain data.
    """
    entry = index[bin_file_name]
    entry["densor_registers"] = {}
    entry["densor_registers"]["rc_enabled"] = rc_enabeled
    entry["densor_registers"]["sensor_states"] = {}
    entry["densor_registers"]["sensor_states"]["temp"] = sensor_states[0]
    entry["densor_registers"]["sensor_states"]["pd"] = sensor_states[1]
    entry["densor_registers"]["sensor_states"]["touch"] = sensor_states[2]
    entry["densor_registers"]["sensor_states"]["baro"] = sensor_states[3]
    entry["densor_registers"]["sensor_states"]["accel"] = sensor_states[4]
    entry["densor_registers"]["start_time"] = start_time
    entry["densor_registers"]["rtc_interval"] = rtc_interval
    entry["densor_registers"]["startup_delay"] = startup_delay
    entry["densor_registers"]["memory_pointer"] = memory_pointer

    with open('index.json', 'w', encoding='utf-8') as f:
        json.dump(index, f, ensure_ascii=False, indent=4)


def label_data(bin_file_path):
    """
    Loads a given binary file from the Densor, labels its samples and stores it in a new csv file, if the binary file has not been processed already, e.g. if the csv does not exist. Also calls "save_settings_index" to store the settings registers in the binary file to the index.

    Parameters
    ----------
    bin_file_path : str
        Path to the binary file to be processed.
    """
    # Obtain the file name and create a path to the new csv file.
    bin_file_name = bin_file_path.split('/')[-1]
    csv_file_path = f"./labeled_data/{bin_file_name.split('.')[0]}.csv"
    print(f"filename: {bin_file_name}")
    print(f"csv path: {csv_file_path}")

    # If the csv file exists, skip file.
    if Path(csv_file_path).is_file():
        print("labeled file already exists, skipping!")
        return

    # Load data from binary file.
    rc_enabled, sensor_states, start_time, rtc_interval_bcd, startup_delay, memory_pointer, readings = plot_readings.read_data(bin_file_path)

    # Only proceed if the binary file can be found in the index.
    if bin_file_name in index:
        # If set, compensate for voltage drop.
        if compensate:
            readings[0][0] = plot_readings.compensate_temp(readings[0][0], readings[5])
            readings[1][0] = plot_readings.compensate_pd(readings[1][0], readings[5])

        # Convert interval to seconds.
        rtc_interval = (((rtc_interval_bcd & 0b01110000) >> 4) * 10) + (rtc_interval_bcd & 0b00001111)
        rtc_interval = rtc_interval * 60 if (rtc_interval_bcd & 0b10000000) else rtc_interval

        # Obtain the protocol per sample.
        protocol = index[bin_file_name]["protocol"]
        print(f"Protocol: {protocol}")
        beggining_seconds_to_skip = 8
        end_seconds_to_skip = 5
        protocol_ps = []
        for instr,secs in protocol:
            for i in range(secs):
                if 'Sleep' in instr:
                    if ((i < (secs - end_seconds_to_skip)) and (i > beggining_seconds_to_skip)):
                        protocol_ps.append(instr)
                    else:
                        protocol_ps.append('unknown')
                else:
                    if (i < (secs - end_seconds_to_skip)):
                        protocol_ps.append(instr)
                    else:
                        protocol_ps.append('unknown')

        # Get the minimum data length, either for data dump or protocol.
        data_len = int(len(protocol_ps) / rtc_interval)
        for s_idx, sensor_state in enumerate(sensor_states):
            if sensor_state:
                data_len = min(data_len, len(readings[s_idx][0]))

        out = [["label"]] + [[protocol_ps[i * rtc_interval]] for i in range(data_len)]
        
        for s_idx, sensor_state in enumerate(sensor_states):
            if sensor_state:
                # Make sure the data_len can never cause idx out-of-bounds when protocol is longer then reading. Should never happen...
                for ax_idx, ax in enumerate(readings[s_idx]):
                    out[0].append(sensor_labels[s_idx][ax_idx])
                    for dp_idx in range(1, data_len + 1):
                        out[dp_idx].append(ax[dp_idx - 1])
        
        # Add VDDA if temperature or photodiode is enabled
        if sensor_states[0] or sensor_states[1]:
            out[0].append(sensor_labels[5][0])
            ax = readings[5][0]
            for dp_idx in range(1, data_len + 1):
                out[dp_idx].append(ax[dp_idx - 1])

        # Write labeled data to csv file.
        with open(csv_file_path,'w') as f:
            writer = csv.writer(f)
            for row in out:
                writer.writerow(row)
            print('Saved '+csv_file_path)

        # Save settings from binary file to index file.
        save_settings_index(bin_file_name, rc_enabled, sensor_states, start_time, rtc_interval, startup_delay, memory_pointer)

def label_csv(csv_file_path):
    """
    Loads a given csv file from the ear-mounted accelerometer, labels its samples and stores it in a the same csv file.

    Parameters
    ----------
    csv_file_path : str
        Path to the csv file to be processed.
    """
    # Obtain the file name of the corresponding Densor binary file to search for protocol in the index file.
    csv_file_name = csv_file_path.split('/')[-1].split('.')[0]
    print(f"File: {csv_file_path}")
    index_csv = index[f"{csv_file_name}.bin"]
    protocol = index_csv["protocol"]

    # Create protocol per sample.
    protocol_ps = [instr for instr,secs in protocol for i in range(secs)]
    
    header = []
    rows_orig = []
    # Load the csv file and label the data.
    with open(csv_file_path, 'r') as csv_file:
        r = csv.reader(csv_file)
        header = next(r)

        # If already labeled, skip.
        if "label" in header:
            print(f"Csv file {csv_file_name} is already labeled. Skipping!")
            return
        
        print(f"Protocol: {protocol}")
        header.append("label")

        rows_orig = list(r)
        csv_start = int(float(rows_orig[0][0]))
        exp_start = index_csv["densor_registers"]["start_time"]

        print(f"CSV start: {csv_start}\nBIN start: {exp_start}")
        protocol_ps = ["unknown" for _ in range(csv_start, exp_start)] + protocol_ps

        for row in rows_orig:
            sec_idx = int(float(row[0])) - csv_start

            if sec_idx < len(protocol_ps):
                row.append(protocol_ps[sec_idx])
            else:
                row.append("unknown")
    
    # Save labeled data to csv file.
    with open(csv_file_path,'w') as csv_file:
        writer = csv.writer(csv_file)
        rows_orig = [header] + rows_orig

        writer.writerows(rows_orig)


if __name__ == "__main__":
    """
    Entry point of the program. Labels a given densor or ear-mounted accelerometer file (or all files in their respective folder when --all argument is supplied).
    """
    parser = argparse.ArgumentParser(description="Script to label a given densor or ear-mounted accelerometer file or all files for a given device.")
    parser.add_argument('--ema', action='store_true',
                    help='Run the script for an ear-mounted accelerometer dataset. If not supplied, will assume a densor binary file is supplied.')
    parser.add_argument('--dont_compensate', action='store_true',
                    help='Does not compensate temperature and photodiode readings based on supply voltage at run-time if supplied.')
    run_mode = parser.add_mutually_exclusive_group()
    run_mode.add_argument('path', type=str, nargs='?',
                    help='Provide a path to a binary file. Path to a CSV file in case --ema is used.')
    run_mode.add_argument('--all', action='store_true',
                    help='Runs the script on all files in either the data_dumps folder or the label_data_ear_mouth folder, depending on if --ema is supplied or not.')
    
    args = parser.parse_args()

    bin_file_path = sys.argv[1]
    index = experiment_master.import_json_file()

    if args.ema:
        if args.all:
            for fn in glob.glob("labeled_data_ear_mouth/*.csv"):
                label_csv(fn)
        else:
            label_csv(args.path)
    else: 
        compensate = not args.dont_compensate
        if args.all:
            for i in index.keys():
                label_data('data_dumps/'+i)
        else:
            label_data(bin_file_path)