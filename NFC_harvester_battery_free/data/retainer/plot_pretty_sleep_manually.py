"""
Script used to generate figure 13 of the paper.
"""
import sys
import matplotlib
import matplotlib.dates as mdate
import matplotlib.pyplot as plt

from datetime import datetime
from matplotlib.lines import Line2D

sys.path.append('../')
from plot_readings import read_data,triginometrie_pitch,triginometrie_roll


date_fmt = '%H:%M:%S'
"""Date-time format used for the x-axis of the graph."""

binary_file_path = "data_dumps/24-04-25-p1-s-01.bin"
"""Path to the Densor binary file to plot."""

rc_enabled, sensor_states, start_time, rtc_interval, startup_delay, memory_pointer, readings = read_data(binary_file_path)

linewidth = 3

print(plt.style.available)
plt.style.use('seaborn-v0_8-ticks')
colors = ['#000000', '#E69F00', '#0072B2', '#56B4E9', '#009E73', '#F0E442', '#D55E00', '#CC79A7']
clr_ind = 1

matplotlib.rcParams.update({'font.size': 15})


# Labels per sensor index.
sensors = { 0: 'Temperature', 1:'Photo-diode', 2:'Touch', 3:'Air pressure', 4:'Acceleration', 5:'VDDA'}

fig,(ax3, ax4, ax5)= plt.subplots(3, 1, figsize=(16, 5.9))

# Time only on last row
ax5.set_xlabel("Time")
date_formatter = mdate.DateFormatter(date_fmt)
ax5.xaxis.set_major_formatter(date_formatter)

# Plot accelerometer graph.
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
    ax3.plot(x,d,color = colors[clr_ind],linewidth = linewidth)
    clr_ind+=1

# Plot head position graph.
ax4.set_title('Head Position')
ax4.set_xticks([])
ax4.set_ylabel(' ')

pitch = [triginometrie_pitch(x_g, y_g, z_g) for (x_g, y_g, z_g) in zip(data_temp[0], data_temp[1], data_temp[2])]
roll = [triginometrie_roll(x_g, y_g, z_g) for (x_g, y_g, z_g) in zip(data_temp[0], data_temp[1], data_temp[2])]

ax4.plot(x,pitch, label="pitch", color=colors[6], linewidth=linewidth)
ax4.plot(x,roll, label="roll", color=colors[7], linewidth=linewidth)

ax4.set_ylim(-180, 180)
ax4.set_yticks([-90, 0, 90])
ax4.set_yticklabels(["left", "center", "right"])

# Plot VDDA graph.
data_temp = readings[5]
ax5.set_title('Supply voltage')
dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
x = mdate.date2num(dt)
ax5.set_ylabel('(V)')
for d in data_temp:
    ax5.plot(x, d, color=colors[1], linewidth=linewidth)
    clr_ind+=1

line_x = Line2D([0], [0], label='X', color=colors[2], linewidth=linewidth)
line_y = Line2D([0], [0], label='Y', color=colors[3], linewidth=linewidth)
line_z = Line2D([0], [0], label='Z', color=colors[4], linewidth=linewidth)
line_roll = Line2D([0], [0], label='Head roll', color=colors[7], linewidth=linewidth)
line_pitch = Line2D([0], [0], label='Head pitch', color=colors[6], linewidth=linewidth)

plt.legend(handles=[line_x, line_y, line_z, line_roll, line_pitch], loc='upper center', ncols=5, bbox_to_anchor=(0.5, -0.5)) 

fig.tight_layout()

if len(sys.argv) > 1:
    filename = sys.argv[1]
    save_path = './plots/{}.pdf'.format(filename)
    save_path_png = './plots/{}.png'.format(filename)
    print(save_path)
    fig.savefig(save_path)
    fig.savefig(save_path_png)
plt.show()
