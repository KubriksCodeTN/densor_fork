"""
Program to plot binary files produced by the Densor.
"""

import copy
import matplotlib
import matplotlib.pyplot as plt
import matplotlib.dates as mdate
import math
import os
import PySimpleGUI as sg

from datetime import datetime
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
from matplotlib.colors import ListedColormap

matplotlib.use('TkAgg')

graph_size = (900, 900)
"""Size of the graph shown on screen."""
list_size = (45, 50)
"""Size of the list interface showing the readings."""
date_fmt = '%d-%m-%y %H:%M:%S'
"""String format used for data time."""
data_start_addr = 9
"""Start of the data section in the binary files."""

mem_size = 8192
"""Maximum size of the binary files in bytes"""
data_sizes = [2, 2, 2, 2, 2]
"""Size of a single reading per sensor"""
stepsize_temp_comp = 0.14
"""Step size for temperature compenstation per 0.1 V"""

def read_data(path):
    """
    Load a densor binary file from a given path, obtain the registers and then read the data and convert it from bytes to python types.
    
    Does convert all values to correct values (for example, accelometer readings are returned in g), but does not compensate for voltage drop.

    Parameters
    ----------
    path : str
        Path to the densor binary file.

    Returns
    -------
    rc_enabled : bool
        True if the RTC used RC mode, False if the RTC used XT mode.
    sensor_states_array : list of bool
        List of booleans, describing which sensors where turned on. Order: Temperature, Photodiode, future1, future2, Accelerometer.
    start_time : int
        Second epoch when the Densor was started. Time of the first reading.
    rtc_interval : int
        Interval between readings in seconds.
    startup_delay : int
        Delay before the first reading in minutes.
    memory_pointer : int
        Pointer to the first memory address that does not contain data.
    readings : list of list of list of Any
        3D list containing all sensor readings.
        Example: [[[Temperature]], [[Photodiode]], [[Future1]], [[Future2]], [[Acceleration X], [Acceleration Y], [Acceleration Z]], [[VDDA]]].
    """

    temp_reading = [[]]
    pd_reading = [[]]
    future1_reading = [[], [], [], [], []]
    future2_reading = [[]]
    accel_reading = [[], [], []]
    vdda_reading = [[]]

    sensor_states = 0
    start_time = 0
    rtc_interval_bcd = 0
    startup_delay = 0
    memory_pointer = 0

    with open(path, "rb") as f:
        sensor_states = int.from_bytes(f.read(1), byteorder='little')
        start_time = int.from_bytes(f.read(4), byteorder='big')
        rtc_interval_bcd = int.from_bytes(f.read(1), byteorder='little')
        startup_delay_bcd = int.from_bytes(f.read(1), byteorder='little')
        memory_pointer = int.from_bytes(f.read(2), byteorder='little')

        print("Loaded binary file...")
        print(f"Sensor states: {sensor_states}")
        print(f"Start time: {start_time}")
        print(f"RTC interval BCD: {rtc_interval_bcd}")
        print(f"Startup delay BCD: {startup_delay}")
        print(f"Memory pointer: {memory_pointer}")

        rtc_interval = (((rtc_interval_bcd & 0b01110000) >> 4) * 10) + (rtc_interval_bcd & 0b00001111)
        rtc_interval = rtc_interval * 60 if (rtc_interval_bcd & 0b10000000) else rtc_interval

        startup_delay = (((startup_delay_bcd & 0b01110000) >> 4) * 10) + (startup_delay_bcd & 0b00001111)

        rc_enabled = bool(sensor_states & 0b10000000)
        temp_enable = bool(sensor_states & 0b00100000)
        pd_enable = bool(sensor_states  & 0b00001000)
        future1_enable = bool(sensor_states & 0b00000100)
        future2_enable = bool(sensor_states & 0b00000010)
        accel_enable = bool(sensor_states & 0b00000001)

        sensor_states_array = [temp_enable, pd_enable, future1_enable, future2_enable, accel_enable]

        block_size = calculate_block_size(sensor_states_array)

        pointer = data_start_addr

        while (pointer < memory_pointer and pointer + block_size < mem_size):
            if temp_enable:
                curr_temp_LSB = int.from_bytes(f.read(2), byteorder='little', signed=True)
                curr_vdda = curr_temp_LSB & 0x0F
                curr_temp_LSB = curr_temp_LSB >> 4
                temp_reading[0].append((curr_temp_LSB / 16) + 25)
                vdda_reading[0].append((curr_vdda + 18) / 10)
                pointer += 2
            elif pd_enable:
                # If temp is not enabled, but pd is enabled, retrieve VDDA. If temp is enabled, pd can use vdda whether it is enabled or not.
                curr_vdda = int.from_bytes(f.read(1), byteorder='little')
                vdda_reading[0].append((curr_vdda + 18) / 10)
                pointer += 1
            if pd_enable:
                pd_reading[0].append(int.from_bytes(f.read(2), byteorder='little'))
                pointer += 2
            if future1_enable:
                for i in range(5):
                    future1_reading[i].append(int.from_bytes(f.read(2), byteorder='little'))
                    pointer += 2
            if future2_enable:
                future2_reading[0].append(int.from_bytes(f.read(2), byteorder='little'))
                pointer += 2
            if accel_enable:
                for i in range(3):
                    accel_reading[i].append(int.from_bytes(f.read(2), byteorder='little',signed = True))
                    pointer += 2

    print("Done reading!")

    return rc_enabled, sensor_states_array, start_time, rtc_interval, startup_delay, memory_pointer, [temp_reading, pd_reading, future1_reading, future2_reading, accel_reading, vdda_reading]

def calculate_block_size(sensor_states):
    """
    Calculate the size (in bytes) of a single sample for all sensors that are enabled.

    Parameters
    ----------
    sensor_states : list of bool
        List of booleans, describing which sensors where turned on. Order: Temperature, Photodiode, future1, future2, Accelerometer.

    Returns
    -------
    block_size : int
        Size of the single sample.
    """

    temp_enable, pd_enable, future1_enable, future2_enable, accel_enable = sensor_states

    block_size = 0
    block_size += data_sizes[0] if temp_enable else 0
    block_size += data_sizes[1] if pd_enable else 0
    block_size += (data_sizes[2] * 5) if future1_enable else 0
    block_size += data_sizes[3] if future2_enable else 0
    block_size += (data_sizes[4] * 3) if accel_enable else 0
    # In case pd is enabled but temp not, vdda is added separately. Otherwise vdda is embedded in temp
    block_size += 1 if (pd_enable and not temp_enable) else 0

    return block_size

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

def triginometrie_roll(x, y, z):
    """
    Calculate the roll (rotation around the x-axis) using triginometrie and the x, y and z values from the accelerometer.

    Returns 0 if it can not be calculated due to division by zero.

    Parameters
    ----------
    x : float
        Acceleration in the x axis in g.
    y : float
        Acceleration in the y axis in g.
    z : float
        Acceleration in the z axis in g.
    
    Returns
    -------
    out : float
        Roll (rotation around the x-axis).
    """

    sign = 1 if z >= 0 else -1
    if sign * math.sqrt(x ** 2 + z ** 2) == 0:
        return 0
    out = ((math.atan2((-1 * y), sign * math.sqrt(x ** 2 + z ** 2))) * 180 / math.pi)
    print(f"Roll: {out}")
    return out

def triginometrie_pitch(x, y, z):
    """
    Calculate the pitch (rotation around the y-axis) using triginometrie and the x, y and z values from the accelerometer.

    Returns 0 if it can not be calculated due to division by zero.

    Parameters
    ----------
    x : float
        Acceleration in the x axis in g.
    y : float
        Acceleration in the y axis in g.
    z : float
        Acceleration in the z axis in g.
    
    Returns
    -------
    out : float
        Pitch (rotation around the y-axis).
    """

    if math.sqrt((- 1 * y) ** 2 + z ** 2) == 0:
        return 0
    pitch = (math.atan2(x, math.sqrt((- 1 * y) ** 2 + z ** 2))) * 180 / math.pi
    print(f"Pitch: {pitch}")
    return pitch

def triginometrie_yaw(x, y, z):
    """
    Calculate the yaw (rotation around the z-axis) using triginometrie and the x, y and z values from the accelerometer.

    Returns 0 if it can not be calculated due to division by zero.

    Parameters
    ----------
    x : float
        Acceleration in the x axis in g.
    y : float
        Acceleration in the y axis in g.
    z : float
        Acceleration in the z axis in g.
    
    Returns
    -------
    out : float
        Yaw (rotation around the z-axis).
    """

    if math.sqrt(x ** 2 + ((- 1 * y) ** 2)) == 0:
        return 0
    return math.atan(z / math.sqrt(x ** 2 + ((- 1 * y) ** 2))) * 180 / math.pi

def compensate_temp(data, vdda_readings):
    """
    Compenstate the temperature readings based on the supply voltage at runtime.

    Parameters
    ----------
    data : list of list of list of Any
        List containing all readings for all sensors.
    vdda_readings : list
        List containing supply voltage per sample.

    Returns
    -------
    list
        List containing all readings for all sensors, with compensated temperature readings.

    """

    return [d - (((vdda_readings[0][i] * 10) - 25) * stepsize_temp_comp) for i, d in enumerate(data)]

def compensate_pd(data, vdda_readings):
    """
    Compenstate the photodiode readings based on the supply voltage at runtime.

    Parameters
    ----------
    data :  list of list of list of Any
        List containing all readings for all sensors.
    vdda_readings : list
        List containing supply voltage per sample.

    Returns
    -------
    list
        List containing all readings for all sensors, with compensated photodiode readings.

    """

    return [(d * vdda_readings[0][i] * 1000) / 4095 for i, d in enumerate(data)]

def plot_graph(data_in, list_interface, rtc_interval, start_time, title, y_label, vdda_readings, vc_enabled, sensor_selected):
    """
    Plot a given dataset on the canvas. Also create a human readable list of readings and plot this on screen.

    Parameters
    ----------
    data_in : list of list of list of any
        List containing all readings for all sensors.
    list_interface
        Pysimplegui interface where the list of readings should be shown.
    rtc_interval : int
        Interval between readings in seconds.
    start_time : int
        Start time of the dataset in seconds Epoch.
    title : str
        Title for the graph
    y_label : str
        Label for the y-axis of the graph
    vdda_readings : list
        List containing supply voltage per sample.
    vc_enabled : bool
        If voltage compensation should be applied on the dataset
    sensor_selected : list
        List of booleans, describing which sensors where turned on. Order: Temperature, Photodiode, future1, future2, Accelerometer.
    """

    fig = plt.figure(1)
    plt.clf()
    data = copy.deepcopy(data_in)
    data_list = []
    cmap = ListedColormap(['red','green'])

    if vc_enabled:
            print("Compensating")
            data[1][0] = compensate_pd(data[1][0], vdda_readings)
            data[0][0] = compensate_temp(data[0][0], vdda_readings)

    if sensor_selected != 6:
        
        dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data[sensor_selected][0]))]
        x = mdate.date2num(dt)
        hrx = [i.strftime(date_fmt) for i in dt]

        ax = fig.add_subplot()

        ax.set_title(title)
        ax.set_xlabel("Time (d-m-y h:m:s)")

        ax.set_ylabel(y_label)
        if dt:
            ax.set_xlim(dt[0], dt[-1], auto=True)
        acc_labels = ['X','Y','Z','Resultant']

        if title == 'Acceleration':
            if len(data[sensor_selected]) == 3:
                for i in range(3):
                    for j in range(len(data[sensor_selected][i])):
                        data[sensor_selected][i][j] = data[sensor_selected][i][j] /16384
                resultant = []
                for i in range(len(data[sensor_selected][0])):
                    r = math.sqrt(data[sensor_selected][0][i]**2 + data[sensor_selected][1][i]**2 + data[sensor_selected][2][i]**2)
                    resultant.append(r)
                data[sensor_selected].append(resultant)

        print(data[sensor_selected])

        if title == "Temperature":
            ax.set_ylim(19, 40)

        if title == "Supply voltage":
            ax.set_ylim(1.8, 3.3)

        future1_labels = ['0','1','2','3','4']
        accel_colors = ['r', 'g', 'b', 'c']
        for i in range(len(data[sensor_selected])):
            d = data[sensor_selected][i]
            if title == 'Acceleration':
                ax.plot(x, d, label = acc_labels[i], color=accel_colors[i])
            elif title == 'future1':
                ax.plot(x, d, label = future1_labels[i])
            else:
                ax.plot(x,d)

            if title == 'Acceleration':
                ax.plot_date(x, d, color=accel_colors[i])
            else:
                ax.plot_date(x, d)
        if title == 'Acceleration' or 'future1':
            ax.legend(loc='center left', bbox_to_anchor=(1, 0.5))
        date_formatter = mdate.DateFormatter(date_fmt)
        ax.xaxis.set_major_formatter(date_formatter)
        if title == 'Photodiode':
            ax.set_ylim((0,3000))

        data_list = hrx
        for d in data[sensor_selected]:
            data_list = ["{0}\t{1}".format(s, i) for (s, i) in list(zip(data_list, d))]
    else:
        ax1 = fig.add_subplot(5, 1, 1)
        ax2 = fig.add_subplot(5, 1, 2)
        ax3 = fig.add_subplot(5, 1, 3)
        ax4 = fig.add_subplot(5, 1, 4)
        ax5 = fig.add_subplot(5, 1, 5)

        date_formatter = mdate.DateFormatter(date_fmt)
        ax5.xaxis.set_major_formatter(date_formatter)
        
        data_temp = data[0]
        print(f"Temp: {data_temp}")
        ax1.set_title('Temperature')
        ax1.set_xticks([])
        ax1.set_ylabel('$(^\circ$Celsius)')
        dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
        x = mdate.date2num(dt)
        wearing = [0 if i < 29 else 1 for i in data_temp[0]]
        for d in data_temp:
            ax1.plot(x,d)

        data_temp = data[1]
        print(f"PD: {data_temp}")
        ax2.set_title('Light intensity')
        ax2.set_xticks([])
        ax2.set_ylabel('Magnitude')
        dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
        x = mdate.date2num(dt)
        for d in data_temp:
            ax2.plot(x,d)

        data_temp = data[4]
        ax3.set_title('Accelerometer')
        ax3.set_xticks([])
        dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
        x = mdate.date2num(dt)
        ax3.set_ylabel('(g)')
        for d in data_temp:
            for i in range(len(d)):
                d[i] = d[i]/16384
            ax3.plot(x,d)

        ax4.set_title('Angle')
        ax4.set_xticks([])
        ax4.set_ylabel('(degree)')
        pitch = [triginometrie_pitch(x_g, y_g, z_g) for (x_g, y_g, z_g) in zip(data_temp[0], data_temp[1], data_temp[2])]
        roll = [triginometrie_roll(x_g, y_g, z_g) for (x_g, y_g, z_g) in zip(data_temp[0], data_temp[1], data_temp[2])]
        ax4.plot(x,pitch,label="pitch")
        ax4.plot(x,roll,label="roll")
        ax4.set_ylim(-180, 180)
        ax4.set_yticks([-90, 0, 90])
        ax4.set_yticklabels(["left", "center", "right"])
        ax4.legend(loc='center right')

        data_temp = data[5]
        ax5.set_title('VDDA')
        dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
        x = mdate.date2num(dt)
        ax5.set_ylabel('(V)')
        for d in data_temp:
            ax5.plot(x,d)

        ax1.pcolor(x, ax1.get_ylim(), [wearing, wearing], alpha=.1, antialiased=True, cmap=cmap)
        ax2.pcolor(x, ax2.get_ylim(), [wearing, wearing], alpha=.1, antialiased=True, cmap=cmap)
        ax3.pcolor(x, ax3.get_ylim(), [wearing, wearing], alpha=.1, antialiased=True, cmap=cmap)
        ax4.pcolor(x, ax4.get_ylim(), [wearing, wearing], alpha=.1, antialiased=True, cmap=cmap)
        ax5.pcolor(x, ax5.get_ylim(), [wearing, wearing], alpha=.1, antialiased=True, cmap=cmap)

        hrx = [i.strftime(date_fmt) for i in dt]
        data_list = hrx
        data_list = ["{0}\t{1}".format(s, i) for (s, i) in list(zip(data_list, pitch))]
        data_list = ["{0}\t{1}".format(s, i) for (s, i) in list(zip(data_list, roll))]

            
    fig.autofmt_xdate()

    fig.canvas.draw()

    list_interface.update(data_list)

def main():
    """
    Main class that creates the GUI interface and handles pysimplegui events.
    """
    # Layouts
    layout_registers = [
        [sg.Text("Filename: "), sg.Text("", key="-FILENAME-")],
        [sg.Input(size=(25, 1), enable_events=True, key="-BIN-PATH-", visible=True), sg.FileBrowse(button_text="Select binary")],
        [sg.HSeparator()],
        [
            sg.Column([
                [sg.Text("RTC clock mode: ")],
                [sg.Text("Sensors enabled: ")],
                [sg.Text("\tTemperature")],
                [sg.Text("\tPhotodiode")],
                # [sg.Text("\tfuture1")],
                # [sg.Text("\tfuture2")],
                [sg.Text("\tAccelerometer")],
                [sg.Text("")],
                [sg.Text("Start timestamp")],
                [sg.Text("RTC sleep interval")],
                [sg.Text("Startup delay")],
                [sg.Text("Memory pointer (EOD)")],
            ]),
            sg.Column([
                [sg.Text("-", key="-ENABLED-RC-")],
                [sg.Text("")],
                [sg.Text("off", key="-ENABLED-TEMP-")],
                [sg.Text("off", key="-ENABLED-PD-")], 
                [sg.Text("off", key="-ENABLED-ACCEL-")],
                [sg.Text("")],
                [sg.Text("0",  key="-REG-TS-")],
                [sg.Text("0",  key="-REG-RTC-INT-")],
                [sg.Text("0",  key="-REG-SU-")],
                [sg.Text("0",  key="-REG-MEM-POINT-")]
            ])
        ],
        [sg.HSeparator()],
        [sg.Text("Settings:")],
        [sg.Text("")],
        [sg.Checkbox("Voltage compensation", enable_events=True, key="-EN-VC-", default=True)]
    ]

    layout_readings = [
        [
            sg.Text("Sensor: "), 
            sg.Radio("Temperature", "sensor_set", enable_events=True, size=(15,1), key="-SENSOR-SET-TEMP-", default=True),
            sg.Radio("Photodiode", "sensor_set", enable_events=True, size=(15,1), key="-SENSOR-SET-PD-"),
            # sg.Radio("future1", "sensor_set", enable_events=True, size=(15,1), key="-SENSOR-SET-future1-"),
            # sg.Radio("future2", "sensor_set", enable_events=True, size=(15,1), key="-SENSOR-SET-future2-"),
            sg.Radio("Accelerometer", "sensor_set", enable_events=True, size=(15,1), key="-SENSOR-SET-ACCEL-"),
            sg.Radio("VDDA", "sensor_set", enable_events=True, size=(15,1), key="-SENSOR-SET-VDDA-"),
            sg.Radio("Paper", "sensor_set", enable_events=True, size=(15,1), key="-SENSOR-SET-PAPER-"),
        ],
        [sg.Text("Not available", visible=True, key="-AVAILABLE-")],
        [
            sg.Graph(graph_size, (0,0), graph_size, key="-GRAPH-"),
            sg.Listbox(values=[], size=list_size, key='-LIST-', horizontal_scroll=True)
        ],

    ]

    # Total layout of the program.
    layout = [
        [      
            sg.Column(layout_readings),
            sg.Column(layout_registers)
        ]
    ]

    # Create the window.
    window = sg.Window("Densor Plotter", layout, finalize=True)
    window.Maximize()

    # Initialize all necessary variables
    binary_file_path = ""
    canvas_interface = window["-GRAPH-"].Widget
    list_interface = window["-LIST-"]

    sensor_selected = 0
    rc_enabled = False
    sensor_states = [False, False, False, False, False]
    start_time = 0
    rtc_interval = 0
    startup_delay = 0
    first_reading_time = 0
    memory_pointer = 0
    readings = []
    update_window = False
    y_label = "Temperature (*C)"
    title = "Temperature"
    vc_enabled = True

    plt.ioff()
    fig = plt.figure(1)
    plt.subplots()

    fig.set_size_inches(10, 7)

    pack_figure(canvas_interface, fig)

    # UI event loop. Acts on events (button presses etc...)
    while True:
        event, values = window.read(timeout=20)

        # Close if event is exit.
        if event == "Exit" or event == sg.WINDOW_CLOSED:
            break

        # Binary path changed, e.g. new binary file is selected and needs to be loaded.
        if event == "-BIN-PATH-":
            p = values["-BIN-PATH-"]
            if os.path.isfile(p):
                print("Loading binary file")
                binary_file_path = p
                rc_enabled, sensor_states, start_time, rtc_interval, startup_delay, memory_pointer, readings = read_data(binary_file_path)
                first_reading_time = start_time + (60 * startup_delay)
                print(readings)
                if sensor_selected == 6 or sensor_states[sensor_selected]:
                    window["-AVAILABLE-"].update(visible=False)
                    plot_graph(readings, list_interface, rtc_interval, first_reading_time, title, y_label, readings[5], vc_enabled, sensor_selected)

                window["-ENABLED-RC-"].update("RC" if rc_enabled else "XT")
                window["-ENABLED-TEMP-"].update("on" if sensor_states[0] else "off")
                window["-ENABLED-PD-"].update("on" if sensor_states[1] else "off")
                # window["-ENABLED-future1-"].update("on" if sensor_states[2] else "off")
                # window["-ENABLED-future2-"].update("on" if sensor_states[3] else "off")
                window["-ENABLED-ACCEL-"].update("on" if sensor_states[4] else "off")

                window["-REG-TS-"].update(start_time)
                window["-REG-RTC-INT-"].update(rtc_interval)
                window["-REG-SU-"].update(startup_delay)
                window["-REG-MEM-POINT-"].update(memory_pointer)

                window["-FILENAME-"].update(os.path.basename(p))

        # Toggle voltage compenstation
        if event == "-EN-VC-":
            vc_enabled = values["-EN-VC-"]
            print(f"Switched vc to {vc_enabled}")
            update_window = True

        # User selects the temperature graph screen
        if event == "-SENSOR-SET-TEMP-":
            sensor_selected = 0
            update_window = True
            title = "Temperature"
            y_label = "Temperature (*C)"

        # User selects the photodiode graph screen
        if event == "-SENSOR-SET-PD-":
            sensor_selected = 1
            update_window = True
            title = "Photodiode"
            y_label = "Light intensity"
        
        # User selects the future1 graph screen
        if event == "-SENSOR-SET-future1-":
            sensor_selected = 2
            update_window = True
            title = "future1"
            y_label = "future1"

        # User selects the future2 graph screen
        if event == "-SENSOR-SET-future2-":
            sensor_selected = 3
            update_window = True
            title = "future2"
            y_label = "future2"

        # User selects the accelerometer graph screen
        if event == "-SENSOR-SET-ACCEL-":
            sensor_selected = 4
            update_window = True
            title = "Acceleration"
            y_label = "Acceleration (g)"

        # User selects the supply voltage graph screen
        if event == "-SENSOR-SET-VDDA-":
            sensor_selected = 5
            update_window = True
            title = "Supply voltage"
            y_label = "Voltage (V)"
        
        # User selects the paper graph screen
        if event == "-SENSOR-SET-PAPER-":
            sensor_selected = 6
            update_window = True
            title = "Paper"
            y_label = ""

        # Window needs to be update, do so.
        if update_window:
            try:
                plot_graph(readings, list_interface, rtc_interval, first_reading_time, title, y_label, readings[5], vc_enabled, sensor_selected)
            except Exception as error:
                # handle the exception
                print("An exception occurred:", error) 
            window["-AVAILABLE-"].update(visible=(not sensor_selected))
            update_window = False


            
    # When broken out of loop, close window
    window.close()


if __name__ == "__main__":
    main()