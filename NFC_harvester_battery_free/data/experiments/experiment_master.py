"""
Background methods and command-line version of program controlling the experiments. Randomizes tasks, displays them to the user, gives a timer for every task and saves protocols to the index file after running.
"""
import json
import random
import time

from datetime import datetime

default_activities = [[('Wear with mouth closed',30)],
                  [('Wear with mouth open',30)],
                  [('Speaking',30)],
                  [('Get ready to drink',15),('Drink for 5 seconds',5),('Rest with mouth closed',10)]
                  ]
"""Default protocol, used if no protocol file is loaded."""

complete_time_fmt = "%Y-%m-%d, %H:%M:%S"
"""Time format used for storing the time at which the experiment took place."""

def get_metadata():
    """
    Requests the user to input their PID and asks for a description of the experiment.

    Returns
    -------
    person : int
        PID of the user.
    description : str
        Description of the experiment.
    """
    person = int(input("Enter your person id:"))
    description = input("Enter any other description:")
    return person, description

def get_random_protocol(activities=default_activities):
    """
    Takes a protocol, adds a "get ready" task to the front and shuffles the other tasks.

    Parameters
    ----------
    activities : list of list of tuple of str and int, default=default_activities
        Protocol to be shuffled.
    
    Returns
    -------
    protocol : list of list of tuple of str and int
        Shuffled protocol.
    """
    protocol = [('Get ready',30)]
    random.shuffle(activities)

    for activity_list in activities:
        for a in activity_list:
            protocol.append(a)

    return protocol

def countdown_timer(t):
    """
    Starts and displays a countdown timer on the terminal for given amount of seconds. Is blocking.

    Parameters
    ----------
    t : int
        The time in seconds for which the timer should run.
    """
    while t: 
        mins, secs = divmod(t, 60) 
        timer = '{:02d}:{:02d}'.format(mins, secs) 
        print(timer, end="\r") 
        time.sleep(1) 
        t -= 1

def execute_protocol(protocol):
    """
    Displays a given protocol step by step, with a countdown timer, on the terminal.

    Parameters
    ----------
    protocol : list of list of tuple of str and int, default=default_activities
        Protocol to run.
    """
    for (activity,time) in protocol:
        print('{} {} seconds'.format(activity,time))
        countdown_timer(time)

def import_json_file():
    """
    Imports the index file into a dictionary and returns it

    Returns
    -------
    data_loaded : dict
        The index file loaded into a dictionary.
    """
    with open('index.json') as data_file:
        data_loaded = json.load(data_file)

    return data_loaded

def get_experiment_count(pid):
    """
    Loads the index file and determines the experiment count for today for the given PID.

    Parameters
    ----------
    pid : int
        Identification number for which the experiment count for today should be obtained.

    Returns
    -------
    experiment_count : int
        The amount of experiment, test subject with PID already did today.
    """

    data_loaded = import_json_file()

    now = datetime.now() 
    ymd = now.strftime("%Y-%m-%d")

    string_to_search = '{}-p{}'.format(ymd, pid)
    print(data_loaded.keys())
    experiment_count = 0

    for datakey in data_loaded.keys():
        if string_to_search in datakey:
            count = int(datakey.split('-')[-1].split('.')[0])
            if count > experiment_count:
                experiment_count = count

    print('Hi p{}. This will be experiment {} for you today'.format(pid,experiment_count+1))

    return experiment_count
    

def construct_file_name(pid, count):
    """
    Creates the binary file name to which the Densor data should be stored.

    Parameters
    ----------
    pid : int
        The identification number of the test subject wearing the Densor / doing the experiment.
    count : int
        The count of experiments the test subject did already do today.

    Returns
    -------
    bin_file_name : str
        The binary file name to which the Densor data should be saved.
    compete_time : str
        The time at which the experiment was completed.
    """
    count += 1 

    now = datetime.now() # current date and time
    complete_time = now.strftime(complete_time_fmt)
    ymd = now.strftime("%Y-%m-%d")

    bin_file_name = '{}-p{}-e-{}.bin'.format(ymd,pid,count)

    print('Please save data with this filename:',bin_file_name)
    return bin_file_name,complete_time

def save_index_file(this_experiment):
    """
    Save the given experiment to the index file.

    Parameters
    ----------
    this_experiment : dict
        Dictionary containing information about a experiment run that should be saved to the index file.
    """
    save_path = 'index.json'
    with open(save_path) as data_file:
        data_loaded = json.load(data_file)

    data_loaded[this_experiment['filename']] = this_experiment

    with open(save_path, 'w', encoding='utf-8') as f:
        json.dump(data_loaded, f, ensure_ascii=False, indent=4)

    print('Data saved to {}'.format(save_path))

def get_additional_notes():
    """
    Request notes from the user on the terminal and return them.

    Returns
    -------
    notes : str
        Notes input by the user.
    """
    notes = input('Enter additional notes here (if any):')
    return notes

def execute_experiment():
    """
    Runs a new experiment on the terminal with the default protocol.

    Returns
    -------
    experiment : dict
        Dictionary containing information of the experiment that ran.
    """
    experiment = {}
    print('Starting a new experiment')

    # get all the information about person,brace 
    pid,description = get_metadata()
    # create a random protocol
    protocol = get_random_protocol()
    # execute that protocol
    execute_protocol(protocol)

    # read the index json file
    experiment_count = get_experiment_count(pid)

    # print out the binary file name of this experiment 
    bin_file , complete_time = construct_file_name(pid,count=experiment_count)

    notes = get_additional_notes()
    
    # save everything to this experiment's dict
    experiment['pid'] = pid
    experiment['description'] = description
    experiment['protocol'] = protocol
    experiment['filename'] = bin_file
    experiment['datetime'] = complete_time
    experiment['notes'] = notes

    # write it back to the index file
    save_index_file(experiment)
    return experiment


if __name__ == '__main__':
    execute_experiment()





    

