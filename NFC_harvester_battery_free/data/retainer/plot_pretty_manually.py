"""
Script used to generate figure 7 of the paper.
"""
import matplotlib
import matplotlib.dates as mdate
import matplotlib.patches as mpatches 
import matplotlib.pyplot as plt
import sys

from datetime import datetime

sys.path.append('../')
from plot_readings import read_data

date_fmt = '%H:%M:%S'
"""Date-time format used for the x-axis of the graph."""

crop_start = 10
"""Sample at which the graph should start"""
crop_end = 40
"""Sample at which the graph should end"""

binary_file_path = "data_dumps/24-04-25-p2-e-11.bin"
"""Path to the Densor binary file to plot."""

rc_enabled, sensor_states, start_time, rtc_interval, startup_delay, memory_pointer, readings = read_data(binary_file_path)

linewidth = 3

light_gray = '#FFB6C1'
light_blue = '#ADD8E6'


plt.style.use('seaborn-v0_8-ticks')
colors = ["#B565A7", '#E69F00', '#0072B2', '#56B4E9', '#DD4124', '#009E73', '#D55E00', '#CC79A7']
color_background = [
    "#785EF0",  # Light Green
    "#DC267F",  # Light Blue
    "#FE6100",  # Light Yellow
    "#648FFF",  # Light Pink
    "#FFB000",  # Light Cyan
    "#0072B2",  # Light Orange
    "#D55E00",  # Pastel Pink
    "#CC79A7"   # Pastel Purple
]
"""Colors used for the graph."""
clr_ind = 1

matplotlib.rcParams.update({'font.size': 15})

# Labels per sensor index. 
sensors = { 0: 'Temperature', 1:'Photo-diode', 2:'Touch', 3:'Air pressure', 4:'Acceleration', 5:'VDDA'}

vdda_readings = readings[5][0]

fig,(ax1, ax2, ax3, ax4)= plt.subplots(4, 1, figsize=(16, 6))#5.9*(4/3)))

def offset(mynum):
    base = 19838.85
    return base + mynum/1000

# Time only on last row
ax4.set_xlabel("Time (hh:mm:ss)")
date_formatter = mdate.DateFormatter(date_fmt)
ax4.xaxis.set_major_formatter(date_formatter)

# Plot temperature graph.

data_temp = readings[0]
ax1.set_title('Temperature',pad = 24)
ax1.set_xticks([])
ax1.set_yticks([26,36])
ax1.set_ylabel('$^\circ$Celsius')
t_upper = 39
ax1.set_ylim((25,t_upper))



dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
x = mdate.date2num(dt)

for d in data_temp:
    ax1.plot(x[crop_start:-crop_end], d[crop_start:-crop_end], color='#0072B2', linewidth=linewidth)

# Plot photodiode graph.
data_temp = readings[1]
ax2.set_title('Light intensity')
ax2.set_xticks([])
ax2.set_ylabel('Magnitude')
ax2.set_ylim((300,950))

dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
x = mdate.date2num(dt)
clr_ind += 1

for d in data_temp:
    d = [(d_i * vdda_readings[i] * 1000) / 4095 for i, d_i in enumerate(d)]
    ax2.plot(x[crop_start:-crop_end], d[crop_start:-crop_end], color='#0072B2', linewidth=linewidth)

# Plot accelerometer graph.
data_temp = readings[4]
ax3.set_title('Acceleration')
ax3.set_xticks([])
ax3.set_yticks([-1,1])
ax3.set_ylim((-2.5,2.5))

dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
x = mdate.date2num(dt)
ax3.set_ylabel('g')
clr_ind += 1

for d in data_temp:
    for i in range(len(d)):
        d[i] = d[i] / 16384
    ax3.plot(x[crop_start:-crop_end], d[crop_start:-crop_end], color=colors[clr_ind], linewidth=linewidth)
    clr_ind+=1

# Plot vdda graph.
data_temp = readings[5]
ax4.set_title('Supply voltage')
ax4.set_ylim((1.8,2.8))
ax4.set_ylabel('V')

dt = [datetime.fromtimestamp((start_time) + (i * rtc_interval)) for i in range(len(data_temp[0]))]
x = mdate.date2num(dt)
clr_ind += 1

for d in data_temp:
    ax4.plot(x[crop_start:-crop_end], d[crop_start:-crop_end], color='#0072B2', linewidth=linewidth)

# ax4.axhline(y=3.3,c='red',linestyle='--')
# ax4.axhline(y=1.8)



def fill_color(t_start,t_end,clr,aph = 0.75):
    """
    Apply the given background color from the given start till the given end.

    Parameters
    ----------
    t_start : float
        Epoch seconds float (including milliseconds) at which the color should start.
    t_end : float
        Epoch seconds float (including milliseconds) at which the color should end.
    clr : str
        The color to apply.
    """
    ax1.axvspan(t_start, t_end, facecolor=clr, alpha=aph)
    ax2.axvspan(t_start, t_end, facecolor=clr, alpha=aph)
    ax3.axvspan(t_start, t_end, facecolor=clr, alpha=aph)
    ax4.axvspan(t_start, t_end, facecolor=clr, alpha=aph)
pd_label_height = 370

label_where_activity = False
if label_where_activity:
    ax1.annotate('Insertion', (offset(3.000),31.42),bbox=dict(facecolor='white', edgecolor='black'))#,(19838.853148-0.00015,31.32+3),bbox=dict(facecolor='white', edgecolor='black'), arrowprops=dict(facecolor='black', width = 1,headwidth = 6, shrink=0.01))
    ax2.annotate('Mouth closed', (offset(3.311),pd_label_height),bbox=dict(facecolor='white', edgecolor='black'))#,(offset(3.358)-0.0002,1200), arrowprops=dict(facecolor='black', width = 1,headwidth = 6, shrink=0.01))
    ax2.annotate('Mouth open', (offset(3.691),pd_label_height),bbox=dict(facecolor='white', edgecolor='black'))#,(offset(3.912)-0.00025,1100), arrowprops=dict(facecolor='black', width = 1,headwidth = 6, shrink=0.01))
    ax2.annotate('Speaking', (offset(4.063),pd_label_height),bbox=dict(facecolor='white', edgecolor='black'))#,(offset(4.157),1500), arrowprops=dict(facecolor='black', width = 1,headwidth = 6, shrink=0.01))
    ax1.annotate('Drinking water', (offset(4.408),27),bbox=dict(facecolor='white', edgecolor='black'))#,(offset(4.199),30), arrowprops=dict(facecolor='black', width = 1,headwidth = 6, shrink=0.01))
    ax1.annotate('Removal', (offset(5.003),27),bbox=dict(facecolor='white', edgecolor='black'))#,(offset(4.830),30),bbox=dict(facecolor='white', edgecolor='black'), arrowprops=dict(facecolor='black', width = 1,headwidth = 6, shrink=0.01))
    ax3.annotate('Tilting head left to right', (offset(4.680),-2.0),bbox=dict(facecolor='white', edgecolor='black'))#,(offset(4.100),-0.68),bbox=dict(facecolor='white', edgecolor='black'), arrowprops=dict(facecolor='black', width = 1,headwidth = 6, shrink=0.01))

label_bottom_axis = False
if label_bottom_axis:
    y_label_height = 1.8
    ax4.annotate('Insertion', (offset(3.057),y_label_height),bbox=dict(facecolor='white', edgecolor='black'))
    ax4.annotate('Mouth closed', (offset(3.290),y_label_height),bbox=dict(facecolor='white', edgecolor='black')) 
    ax4.annotate('Mouth open', (offset(3.691),y_label_height),bbox=dict(facecolor='white', edgecolor='black'))
    ax4.annotate('Speaking', (offset(4.063),y_label_height),bbox=dict(facecolor='white', edgecolor='black'))
    ax4.annotate('Drinking water', (offset(4.408),y_label_height),bbox=dict(facecolor='white', edgecolor='black'))
    ax4.annotate('Removal', (offset(5.003),y_label_height),bbox=dict(facecolor='white', edgecolor='black'))
    ax4.annotate('Tilting head', (offset(4.750),y_label_height),bbox=dict(facecolor='white', edgecolor='black'))

label_top_axis = True

if label_top_axis:
    y_label_height = t_upper
    ax1.annotate('Insertion', (offset(3.057),y_label_height),bbox=dict(facecolor='white', edgecolor='black'))
    ax1.annotate('Mouth closed', (offset(3.290),y_label_height),bbox=dict(facecolor='white', edgecolor='black')) 
    ax1.annotate('Mouth open', (offset(3.691),y_label_height),bbox=dict(facecolor='white', edgecolor='black'))
    ax1.annotate('Speaking', (offset(4.063),y_label_height),bbox=dict(facecolor='white', edgecolor='black'))
    ax1.annotate('Drinking water', (offset(4.408),y_label_height),bbox=dict(facecolor='white', edgecolor='black'))
    ax1.annotate('Removal', (offset(5.003),y_label_height),bbox=dict(facecolor='white', edgecolor='black'))
    ax1.annotate('Tilting head', (offset(4.750),y_label_height),bbox=dict(facecolor='white', edgecolor='black'))
# Fill background per label.
if False:
    fill_color(19838.853090, 19838.853188, color_background[2])
    fill_color(19838.853188, 19838.8536, color_background[1])
    fill_color(19838.8536, 19838.853962, color_background[4])
    fill_color(19838.853962, 19838.854321, color_background[3])
    fill_color(19838.854437, 19838.854577, color_background[0])
    fill_color(19838.854675, 19838.855013, color_background[5])
    fill_color(19838.855013, 19838.855100, color_background[2])
if True:
    fill_color(19838.853050, 19838.853188, light_gray)
    fill_color(19838.853188, 19838.8536, light_blue)
    fill_color(19838.8536, 19838.853962,  light_gray)
    fill_color(19838.853962, 19838.854321, light_blue)
    fill_color(19838.854437, 19838.854577,  light_gray)
    fill_color(19838.854675, 19838.855013, light_blue)
    fill_color(19838.855013, 19838.855100,  light_gray)
# # Creating legend with color box 
# insertion = mpatches.Patch(color=colors[0], alpha=aph, label='Insertion and Removal') 
# m_cld = mpatches.Patch(color=colors[1], alpha=aph, label='Mouth closed') 
# m_opn = mpatches.Patch(color=colors[2], alpha=aph, label='Mouth open') 
# spkng = mpatches.Patch(color=colors[3], alpha=aph, label='Speaking') 
# drkng = mpatches.Patch(color=colors[4], alpha=aph, label='Drinking water') 
# tiltng = mpatches.Patch(color=colors[5], alpha=aph, label='Tilting head from left to right') 
# plt.legend(handles=[insertion, m_cld, m_opn, spkng, drkng, tiltng], loc='upper center', ncols=3, bbox_to_anchor=(0.5, -0.5)) 

fig.tight_layout()

if len(sys.argv) > 1:
    filename = sys.argv[1]
    save_path = './plots/{}.pdf'.format(filename)
    save_path_png = './plots/{}.png'.format(filename)
    print(save_path)
    fig.savefig(save_path)
    fig.savefig(save_path_png)
plt.show()
