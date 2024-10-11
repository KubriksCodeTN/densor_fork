"""
Program to plot labeled Densor data.
"""
import copy
import csv
import experiment_master
import matplotlib
import matplotlib.pyplot as plt
import os
import PySimpleGUI as sg
import sys

from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
from matplotlib.colors import ListedColormap

# Import plot_readings, which is located in the parent directory.
sys.path.append('../')
import plot_readings

matplotlib.use('TkAgg')

graph_size = (900, 900)
"""Size of the graph showing the plotted data."""
list_size = (90, 50)
"""Size of the list displaying the readings."""
event_bg_alpha = .1
"""Alpha value for the background colors of the plot."""
list_font = ("Courier", 12)
"""Font used for the list displaying the readings."""

def isfloat(fl):
    """
    Checks if the given variable is of float type.

    Parameters
    ----------
    fl : Any
        The variable to check.
    
    Returns
    bool
        True if the variable is of type float, False otherwise.
    """
    try:
        float(fl)
        return True
    except ValueError:
        return False
    
def read_data(path):
    """
    Loads a file at given filepath and Deserializes it into lists of readings.

    Parameters
    ----------
    path : str
        Path to the file to load.
    
    Returns
    -------
    curr_index : dict
        Entry of the given file in the index file.
    readings : list of list of list of Any
        3D list containing all sensor readings.
        Example: [[[Temperature]], [[Photodiode]], [[Touch 0], [Touch 1], [Touch 2], [Touch 3], [Touch 4]], [[Barometer]], [[Acceleration X], [Acceleration Y], [Acceleration Z]], [[VDDA]]].
    str_data_list : list of str
        List of strings used to display the readings in the readings list.
    """
    index = experiment_master.import_json_file()
    filename = f"{os.path.basename(path)[:-4]}.bin"
    curr_index = index[filename]

    # temp, pd, touch, baro, accel, vdda, label
    readings = [[[]], [[]], [[], [], [], [], [], []], [[]], [[], [], []], [[]], [[]]]

    with open(path, 'r') as csv_file:
        reader = csv.reader(csv_file, delimiter=',', quotechar='|')
        header = next(reader, None)  # skip the headers
        data_list = []

        for row in reader:
            data_list.append(row)
            readings[6][0].append(row[0])
            i = 1

            if curr_index["densor_registers"]["sensor_states"]["temp"]:
                readings[0][0].append(float(row[i]))
                i += 1

            if curr_index["densor_registers"]["sensor_states"]["pd"]:
                readings[1][0].append(float(row[i]))
                i += 1

            if curr_index["densor_registers"]["sensor_states"]["touch"]:
                readings[2][0].append(float(row[i]))
                readings[2][1].append(float(row[i + 1]))
                readings[2][2].append(float(row[i + 2]))
                readings[2][3].append(float(row[i + 3]))
                readings[2][4].append(float(row[i + 4]))
                i += 5

            if curr_index["densor_registers"]["sensor_states"]["baro"]:
                readings[3][0].append(float(row[i]))
                i += 1

            if curr_index["densor_registers"]["sensor_states"]["accel"]:
                readings[4][0].append(float(row[i]) / 16384)
                readings[4][1].append(float(row[i + 1]) / 16384)
                readings[4][2].append(float(row[i + 2]) / 16384)
                i += 3

            if curr_index["densor_registers"]["sensor_states"]["temp"] or curr_index["densor_registers"]["sensor_states"]["pd"]:
                readings[5][0].append(float(row[i]))
                i += 1

    str_data_list = ["{:33s}".format(header[0])]

    for i in range(1, len(header)):
        str_data_list[0] = "{} \t {:>9s}".format(str_data_list[0], header[i])

    for r in data_list:
        out = ""
        for c in r:
            if c.isdigit():
                out = "{} \t {:9d}".format(out, int(c))
            elif c[1:].isdigit():
                out = "{} \t {:9d}".format(out, int(c))
            elif isfloat(c):
                out = "{} \t {:9.4f}".format(out, float(c))
            else:
                out = "{:33s}".format(c)
        str_data_list.append(out)

    return curr_index, readings, str_data_list

def pack_figure(canvas_interface, fig):
    """
    Pack a given matplotlib figure into a given pysimplegui (TKinter) canvas.

    Parameters
    ----------
    canvas_interface
        The canvas interface from pysimplegui.
    fig
        The fig from matplotlib.
    """
    figure_canvas_agg = FigureCanvasTkAgg(fig, canvas_interface)
    figure_canvas_agg.get_tk_widget().pack(side="top", fill="both", expand=1)

def plot_graph(data_in, curr_index):
    """
    Plot a given dataset on the canvas.

    Parameters
    ----------
    data_in : list of list of list of any
        List containing all readings for all sensors.
    curr_index
        Entry of the data set in the index file.
    """
    fig = plt.figure(1)
    plt.clf()

    data = copy.deepcopy(data_in)
    cmap = ListedColormap(['red','green'])
    sensor_states = curr_index["densor_registers"]["sensor_states"]
    graph_count = sensor_states["temp"] + sensor_states["pd"] + sensor_states["touch"] + sensor_states["baro"] + sensor_states["accel"] + sensor_states["accel"]
    graph_count += 1 if sensor_states["temp"] or sensor_states["accel"] else 0

    graph_idx = 1
    x = range(len(data[6][0]))
    events = [0 if l == "unknown" else 1 for l in data[6][0]]
    events = [1] + events[:-1]
    print(events)

    # Plot the temperature graph
    if sensor_states["temp"]:
        ax = fig.add_subplot(graph_count, 1, graph_idx)
        graph_idx += 1

        data_temp = data[0]

        ax.set_title('Temperature')
        ax.set_xticks([])
        ax.set_ylabel('$(^\circ$Celsius)')

        for d in data_temp:
            ax.plot(x,d)

        ax.pcolor(x, ax.get_ylim(), [events, events], alpha=event_bg_alpha, antialiased=True, cmap=cmap)

    # Plot the photodiode graph
    if sensor_states["pd"]:
        ax = fig.add_subplot(graph_count, 1, graph_idx)
        graph_idx += 1

        data_temp = data[1]

        ax.set_title('Light intensity')
        ax.set_xticks([])
        ax.set_ylabel('Magnitude')
        
        for d in data_temp:
            ax.plot(x,d)

        ax.pcolor(x, ax.get_ylim(), [events, events], alpha=event_bg_alpha, antialiased=True, cmap=cmap)
    
    # Plot the touch graph
    if sensor_states["touch"]:
        ax = fig.add_subplot(graph_count, 1, graph_idx)
        graph_idx += 1

        data_temp = data[2]
        
        ax.set_title('Touch')
        ax.set_xticks([])
        ax.set_ylabel('Magnitude')
        
        for d in data_temp:
            ax.plot(x,d)

        ax.pcolor(x, ax.get_ylim(), [events, events], alpha=event_bg_alpha, antialiased=True, cmap=cmap)

    # Plot the barometer graph
    if sensor_states["baro"]:
        ax = fig.add_subplot(graph_count, 1, graph_idx)
        graph_idx += 1

        data_temp = data[3]
        
        ax.set_title('Barometer')
        ax.set_xticks([])
        ax.set_ylabel('(hPa)')
        
        for d in data_temp:
            ax.plot(x,d)

        ax.pcolor(x, ax.get_ylim(), [events, events], alpha=event_bg_alpha, antialiased=True, cmap=cmap)

    # Plot the accelerometer graph
    if sensor_states["accel"]:
        accel_labels = ["x", "y", "z"]
        ax = fig.add_subplot(graph_count, 1, graph_idx)
        graph_idx += 1

        data_temp = data[4]
        
        ax.set_title('Accelerometer')
        ax.set_xticks([])
        ax.set_ylabel('(g)')
        
        for i, d in enumerate(data_temp):
            ax.plot(x,d, label=accel_labels[i])
        
        ax.pcolor(x, ax.get_ylim(), [events, events], alpha=event_bg_alpha, antialiased=True, cmap=cmap)
        ax.legend(loc='center right')

        ax = fig.add_subplot(graph_count, 1, graph_idx)
        graph_idx += 1

        ax.set_title('Angle')
        ax.set_xticks([])
        ax.set_ylabel('(degree)')

        yaw = [plot_readings.triginometrie_yaw(x_g, y_g, z_g) for (x_g, y_g, z_g) in zip(data_temp[0], data_temp[1], data_temp[2])]
        pitch = [plot_readings.triginometrie_pitch(x_g, y_g, z_g) for (x_g, y_g, z_g) in zip(data_temp[0], data_temp[1], data_temp[2])]
        roll = [plot_readings.triginometrie_roll(x_g, y_g, z_g) for (x_g, y_g, z_g) in zip(data_temp[0], data_temp[1], data_temp[2])]

        ax.plot(x,yaw,label="yaw")
        ax.plot(x,pitch,label="pitch")
        ax.plot(x,roll,label="roll")

        ax.set_ylim(-180, 180)
        ax.set_yticks([-90, 0, 90])
        ax.set_yticklabels(["left", "center", "right"])
        ax.legend(loc='center right')

        ax.pcolor(x, ax.get_ylim(), [events, events], alpha=event_bg_alpha, antialiased=True, cmap=cmap)

    # Plot the VDDA graph
    if sensor_states["temp"] or sensor_states["pd"]:
        ax = fig.add_subplot(graph_count, 1, graph_idx)
        graph_idx += 1

        data_temp = data[5]

        protocol = curr_index["protocol"]
        rtc_interval = curr_index["densor_registers"]["rtc_interval"]

        labels = [name for name, _ in protocol]
        protocol_ticks = []

        start = 0
        for _, secs in protocol:
            protocol_ticks.append(start / rtc_interval)
            start += secs
        
        ax.set_title('Supply Voltage')
        ax.set_xticks(protocol_ticks, labels)
        ax.set_ylabel('(V)')
        
        for d in data_temp:
            ax.plot(x,d)

        ax.pcolor(x, ax.get_ylim(), [events, events], alpha=event_bg_alpha, antialiased=True, cmap=cmap)

        
    fig.autofmt_xdate()

    fig.canvas.draw()

def main():
    """
    Main class that creates the GUI interface and handles pysimplegui events.
    """
    # Layouts

    layout_readings = [
        [sg.Text("Filename: "), sg.Text("", key="-FILENAME-")],
        [sg.Input(size=(25, 1), enable_events=True, key="-CSV-PATH-", visible=True), sg.FileBrowse(button_text="Select CSV")],
        [
            sg.Graph(graph_size, (0,0), graph_size, key="-GRAPH-"),
            sg.Listbox(values=[], size=list_size, key='-LIST-', horizontal_scroll=True, font=list_font)
        ],

    ]

    # Total layout of the program.
    layout = [
        [      
            sg.Column(layout_readings)
        ]
    ]

    # Create the window.
    window = sg.Window("Plot Labeled", layout, finalize=True)
    window.Maximize()

    # Initialize all necessary variables
    csv_file_path = ""
    canvas_interface = window["-GRAPH-"].Widget
    list_interface = window["-LIST-"]
    curr_index = {}
    data_list = []

    readings = []

    plt.ioff()
    fig = plt.figure(1)
    plt.subplots()

    fig.set_size_inches(10, 7)

    pack_figure(canvas_interface, fig)

    # UI event loop. Acts on events (button presses etc...)
    while True:
        # Load events and values.
        event, values = window.read(timeout=20)

        # Close if event is exit.
        if event == "Exit" or event == sg.WINDOW_CLOSED:
            break

        # Events to load the data file.
        if event == "-CSV-PATH-":
            p = values["-CSV-PATH-"]
            if os.path.isfile(p):
                print("Loading csv file")
                csv_file_path = p
                
                curr_index, readings, data_list = read_data(csv_file_path)

                plot_graph(readings, curr_index)
                window["-FILENAME-"].update(os.path.basename(p))
                list_interface.update(data_list)

    # When broken out of loop, close window
    window.close()


if __name__ == "__main__":
    main()

