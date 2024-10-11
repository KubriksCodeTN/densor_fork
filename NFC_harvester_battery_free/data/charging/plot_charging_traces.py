"""
Script to plot charging traces provided in ./raw_charging_traces/ .
"""

import glob
import numpy as np
import pandas as pd

from matplotlib import pyplot as plt


files = glob.glob('**/*.csv',recursive=True)
print(files)
assert files != [], False 

plt.rcParams.update({'font.size': 13})

colors = ['#E69F00','#56B4E9','#0072B2','#D55E00','#000000','#009E73','#CC79A7','#F0E442']
lines = ['solid','dashed','dashdot','dotted']
clr_ind = 0
plt.rcParams.update({'font.size': 14})
plt.style.use('seaborn-v0_8-ticks')
plt.figure(figsize=(5,2.5))

length = 25500
threshold = 0.25
offset = 10

for file in files:

    if 'coffee' in file:
        continue
    if 'nucleo' in file:
        continue
    df = pd.read_csv(file)
    volts = df['Channel 3'].to_numpy()
    start = np.where(volts>threshold)[0][0]
    start = start - offset
    print(start)

    volts = volts[start : start+ length]
    times =  df['Time [s]'].to_numpy()
    times = times[start:start+length]
    print(times)
    times = times - times[offset]

    if 'pixel-6a' in file:
        lab = 'Pixel 6A'
    elif 'iphone-13' in file:
        lab = 'iPhone 13'
    elif 'galaxys8' in file:
        lab = 'Galaxy S8'
    elif 'pixel-3a' in file:
        lab = 'Pixel 3A'
    elif 'coffee' in file:
        lab = 'Generic NFC reader'
    elif 'nfc03a1' in file:
        lab = 'NUCLEO NFC03A1'
    else:
        lab = 'NA'

    
    plt.plot(times, volts, label=lab, linewidth=2, color=colors[clr_ind], linestyle=lines[clr_ind % len(lines)], alpha=0.95)
    clr_ind += 1

plt.xlabel('Time (s)',)
plt.ylabel('Capacitor voltage (V)')

plt.grid(linestyle='dotted')
plt.legend()
plt.margins(0.0)
plt.tight_layout() # to fit everything in the prescribed area
plt.savefig('graphs/phonecharge.pdf')
plt.show()