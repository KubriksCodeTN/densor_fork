"""
User interface for a program used to run experiments. Randomizes tasks, displays them to the user, gives a timer for every task and saves protocols to the index file after running.
"""
import experiment_master
import json
import os
import PySimpleGUI as sg
import time

from threading import Thread

date_fmt = '%d-%m-%y %H:%M:%S'
"""Date-time format used for saving time of experiment."""
list_size = (40, 25)
"""Size of the list displaying tasks and their order and time."""
big_font = ("Calibri", 20)
"""Font for the bigger text in the UI."""
list_font = ("Courier", 12)
"""Font for the text in the tasks list."""
exp_finished_msg = "Experiment finished!"
"""Message that is displayed after all tasks are completed."""

def load_experiment(path):
    """
    Loads an experiment from a given filepath to a json file.

    Parameters
    ----------
    path : str
        Path to a json file to be loaded.
    
    Returns
    -------
    list of list of tuple of str and int
        Deserialized protocol in given json file.
    """
    with open(path) as exp_file:
        return json.load(exp_file)

def update_list(window, protocol):
    """
    Update the tasks list in given window by given protocol.

    Parameters
    ----------
    window : Window
        The current PySimpleGui window.
    protocol : list of list of tuple of str and int
        The protocol to be displayed in the task list.
    """
    out = []
    for (act, time) in protocol:
        mins, secs = divmod(time, 60) 
        out.append("{:<33} {:02d}:{:02d}".format(act, mins, secs))

    window["-LIST-"].update(out)

def reset_window(window):
    """
    Reset the given window to a default display.

    Parameters
    ----------
    window : Window
        The current PySimpleGui window to be updated.
    """
    window["-CURRENT-STEP-"].update("Start experiment!")
    window["-TIMER-"].update("00:00")
    window["-NEXT-STEP-DESC-"].update("Get ready")
    window["-NEXT-STEP-TIME-"].update("00:30")

def run_experiment(window, protocol):
    """
    Starts an experiment. Will go through the given protocol and display them one by one, including a timer that counts down till the end of the task.
    
    Parameters
    ----------
    window : Window
        The current PySimpleGui window to be updated.
    protocol : list of list of tuple of str and int
        The protocol to run.
    """
    for (i, (curr_activity,curr_time)) in enumerate(protocol):
        mins_next, secs_next = (0,0)

        if i + 1 < len(protocol):
            mins_next, secs_next = divmod(protocol[i + 1][1], 60)
            window["-NEXT-STEP-DESC-"].update(protocol[i + 1][0])
        else:
            window["-NEXT-STEP-DESC-"].update(exp_finished_msg)

        window["-CURRENT-STEP-"].update(curr_activity)
        window["-NEXT-STEP-TIME-"].update("{:02d}:{:02d}".format(mins_next, secs_next))

        t = curr_time
        while t: 
            mins, secs = divmod(t, 60) 
            window["-TIMER-"].update('{:02d}:{:02d}'.format(mins, secs) )
            time.sleep(1) 
            t -= 1

    window["-CURRENT-STEP-"].update(exp_finished_msg)
    window["-TIMER-"].update("00:00")
    window["-NEXT-STEP-DESC-"].update("Done! :)")

def main():
    """
    Main class that creates the GUI interface and handles pysimplegui events.
    """

    # Layouts
    layout_settings = [
        [sg.Text("Filename: "), sg.Text("", key="-FILENAME-")],
        [sg.Input(size=(25, 1), enable_events=True, key="-EXP-PATH-", visible=True), sg.FileBrowse(button_text="Select experiment")],
        [sg.HSeparator()],
        [
            sg.Column([
                [sg.Text("PID: ")],
                [sg.Text("Description: ")],
                [sg.Text("Densor")]
            ]),
            sg.Column([
                [sg.Input(size=(25, 1), enable_events=True, key="-PID-INPUT-", visible=True)],
                [sg.Input(size=(25, 1), enable_events=True, key="-DESC-INPUT-", visible=True)],
                [sg.Input(size=(25, 1), enable_events=True, key="-DENSOR-INPUT-", visible=True)]
            ])
        ],
        [sg.HSeparator()],
        [
            sg.Column([
                [sg.Text("Experiment count PID today: ")]
            ]),
            sg.Column([
                [sg.Text("-", key="-EXP-COUNT-")],
            ])
        ],
        [sg.HSeparator()],
        [sg.Button("Shuffle experiment", key="-SHUFFLE-BTN-")],
        [sg.Button("Start experiment", key="-START-BTN-")]
    ]

    layout_exp = [
        [
            sg.Frame("" ,[
                [sg.Text("Current Step:", font=big_font)],
                [sg.Text("Start experiment!", font=big_font, key="-CURRENT-STEP-")],
                [sg.Text("00:00", font=big_font, key="-TIMER-")],
                [sg.HSeparator()],
                [sg.Text("Next Step:", font=big_font)],
                [sg.Text("Get ready", font=big_font, key="-NEXT-STEP-DESC-")],
                [sg.Text("00:30", font=big_font, key="-NEXT-STEP-TIME-")],
                
            ], size=(500, 230), element_justification="center"),
            sg.Listbox(values=["Insert Densor \t\t 00:30", "Drink water \t\t 00:30", "Hello World \t\t 01:00"], size=list_size, key='-LIST-', horizontal_scroll=True, font=list_font)
        ]
    ]

    # Total layout of the program.
    layout = [
        [      
            sg.Column(layout_exp),
            sg.Column(layout_settings)
        ]
    ]

    # Create the window.
    window = sg.Window("Experiment Master", layout, finalize=True)
    window.Maximize()

    # Initialize all necessary variables
    experiment_file_path = ""
    current_pid = -1
    experiment_count = 0
    description = ""
    densor = -1
    activity_list = experiment_master.default_activities
    protocol = experiment_master.get_random_protocol(activity_list)
    update_list(window, protocol)
    t = Thread(target=run_experiment, args=[window, protocol])
    running = False


    # UI event loop. Acts on events (button presses etc...)
    while True:
        # Check for experiment end
        if not t.is_alive() and running:
            running = False
            notes = sg.popup_get_text("Experiment finished. Any notes?", "End of experiment")
            bin_file , complete_time = experiment_master.construct_file_name(current_pid,experiment_count)

    
            # save everything to this experiment's dict
            experiment = {}
            experiment['pid'] = current_pid
            experiment['description'] = description
            experiment['protocol'] = protocol
            experiment['filename'] = bin_file
            experiment['datetime'] = complete_time
            experiment['notes'] = notes

            experiment_master.save_index_file(experiment)
            sg.popup_ok(f"Please save data with this filename: {bin_file}")
            experiment_count = experiment_master.get_experiment_count(current_pid)
            window["-EXP-COUNT-"].update(experiment_count)

        # Load events and values.
        event, values = window.read(timeout=10)

        # Close if event is exit.
        if event == "Exit" or event == sg.WINDOW_CLOSED:
            break
        
        # Path to experiment has changed
        if event == "-EXP-PATH-":
            experiment_file_path = values["-EXP-PATH-"]
            activity_list = load_experiment(experiment_file_path)
            protocol = experiment_master.get_random_protocol(activity_list)
            update_list(window, protocol)
            reset_window(window)
            window["-FILENAME-"].update(os.path.basename(experiment_file_path))

        # PID changed
        if event == "-PID-INPUT-":
            if values["-PID-INPUT-"].isdigit():
                current_pid = int(values["-PID-INPUT-"])
                experiment_count = experiment_master.get_experiment_count(current_pid)
                window["-EXP-COUNT-"].update(experiment_count)
            else:
                densor = -1
                window["-EXP-COUNT-"].update("-")

        if event == "-DESC-INPUT-":
            description = values["-DESC-INPUT-"]

        if event == "-DENSOR-INPUT-":
            if values["-DENSOR-INPUT-"].isdigit():
                densor = int(values["-DENSOR-INPUT-"])
            else:
                densor = -1
        
        # Shuffle button pressed
        if event == "-SHUFFLE-BTN-":
            protocol = experiment_master.get_random_protocol(activity_list)
            update_list(window, protocol)
        
        # Start experiment button pressed
        if event == "-START-BTN-":
            if current_pid == -1 or densor == -1:
                sg.popup_error("First set all settings correctly!")
            else:
                t = Thread(target=run_experiment, args=[window, protocol])
                t.start()
                running = True
        
            
    # When broken out of loop, close window
    window.close()


if __name__ == "__main__":
    """
    Entry point of the program. Start the GUI.
    """
    main()