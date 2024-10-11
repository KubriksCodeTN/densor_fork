"""
Old script that generates a plot containing all traces of the Densor. Only saves the graph if a second argument is provided. Does not apply voltage compensation or roll/pitch calibration.
"""
import matplotlib
import matplotlib.dates as mdate
import matplotlib.pyplot as plt
import sys

from datetime import datetime

sys.path.append('../')
from plot_readings import read_data, triginometrie_pitch, triginometrie_roll

date_fmt = '%d-%m-%y %H:%M:%S'
"""Date-time format used for the x-axis of the plot."""
binary_file_path = sys.argv[1]
"""Path to the densory binary file to plot."""

rc_enabled, sensor_states, start_time, rtc_interval, startup_delay, memory_pointer, readings = read_data(binary_file_path)

linewidth = 3

print(plt.style.available)
plt.style.use('seaborn-v0_8-ticks')
colors = ['#000000', '#E69F00', '#0072B2', '#56B4E9', '#009E73', '#F0E442', '#D55E00', '#CC79A7']
clr_ind = 1

matplotlib.rcParams.update({'font.size': 14})

sensors = { 0: 'Temperature', 1:'Photodiode', 2:'Touch', 3:'Air pressure', 4:'Acceleration', 5:'VDDA'}
"""Titles per sensor index."""

fig,(ax1, ax2, ax3, ax4, ax5)= plt.subplots(5, 1, figsize=(16, 8))

# Time only on last row
ax5.set_xlabel("Ground truth")
date_formatter = mdate.DateFormatter(date_fmt)
ax5.xaxis.set_major_formatter(date_formatter)

data_temp = readings[0]

# Plot temperature graph
ax1.set_title('Temperature')
ax1.set_xticks([])
ax1.set_ylabel('$(^\circ$Celsius)')
dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
x = mdate.date2num(dt)
for d in data_temp:
    ax1.plot(x, d, color=colors[clr_ind], linewidth=linewidth)

data_temp = readings[1]

# Plot light intensity graph
ax2.set_title('Light intensity')
ax2.set_xticks([])
ax2.set_ylabel('Magnitude')
dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
x = mdate.date2num(dt)
clr_ind += 1
for d in data_temp:
    ax2.plot(x, d, color=colors[clr_ind], linewidth=linewidth)

# Plot accelerometer graph
data_temp = readings[4]
ax3.set_title('Accelerometer')
ax3.set_xticks([])
dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
x = mdate.date2num(dt)
ax3.set_ylabel('(g)')
clr_ind += 1
for d in data_temp:
    for i in range(len(d)):
        d[i] = d[i] / 16384
    ax3.plot(x, d, color=colors[clr_ind], linewidth=linewidth)
    clr_ind+=1

# Plot head-position graph
ax4.set_title('Head Position')
ax4.set_xticks([])
ax4.set_ylabel(' ')
pitch = [triginometrie_pitch(x_g, y_g, z_g) for (x_g, y_g, z_g) in zip(data_temp[0], data_temp[1], data_temp[2])]
roll = [triginometrie_roll(x_g, y_g, z_g) for (x_g, y_g, z_g) in zip(data_temp[0], data_temp[1], data_temp[2])]
ax4.plot(x, pitch, label='pitch', color=colors[2], linewidth=linewidth)
ax4.plot(x, roll, label='roll', color=colors[7], linewidth=linewidth)
ax4.set_ylim(-180, 180)
ax4.set_yticks([-90, 0, 90])
ax4.set_yticklabels(["left", "center", "right"])
ax4.legend(loc='center right')

# Plot supply voltage graph
data_temp = readings[5]
ax5.set_title('Supply voltage')
dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
x = mdate.date2num(dt)
ax5.set_ylabel('(V)')
for d in data_temp:
    ax5.plot(x, d, color=colors[clr_ind], linewidth=linewidth)
    clr_ind+=1

fig.tight_layout()

if len(sys.argv) > 2:
    filename = sys.argv[2]
    save_path = './plots/{}.png'.format(filename)
    print(save_path)
    fig.savefig(save_path)

plt.show()
