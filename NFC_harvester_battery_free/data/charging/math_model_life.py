"""
Script to model the lifetime of Densor 
"""


from matplotlib import pyplot as plt

plt.style.use('seaborn-ticks')
plt.figure(figsize=(5,2.5))
colors = ['#56B4E9','#000000','#E69F00','#009E73','#F0E442','#0072B2','#D55E00','#CC79A7']
lines = ['solid','dashed','dashdot','densely dashdotted']
clr_ind = 0
plt.rcParams.update({'font.size': 16})

PLOT_CAPS = True
PLOT_CURRENT = False
PLOT_START_VOLTAGE = False

def get_Iavg(sampling_interval):
    '''Calculates the average current for a given sampling interval'''
    run_time = 0.054
    run_current = 822 * 10**-6
    sleep_time = sampling_interval - run_time
    sleep_current = 22 * 10**-9

    Iavg = ((sleep_time*sleep_current) + (run_time*run_current))/sampling_interval
    print('Sampling interval:{}s Avg current:{:4f} uA'.format(sampling_interval,Iavg*10**6))
    return Iavg

def get_lifetime_emperical(sampling_interval):
    life = (get_Iavg(10)* 5460)/get_Iavg(sampling_interval)
    return life

def get_lifetime(sampling_interval,caps = 2, VH = 2.6):
    '''Calculates the expected lifetime based on input parameters'''
    C =  11 * 10**-3
    VL = 1.8
    num = caps * 0.5 * C * ( VH**2 - VL**2)
    print(num)
    den =  VH * get_Iavg(sampling_interval)
    return num/den

if PLOT_CAPS:
    for caps in range(1,4):

        VH = 3.3
        sampling_intervals = []
        Iavgs = []
        lifes = []

        for sampling_interval in range(1,130):
            sampling_intervals.append(sampling_interval)
            Iavgs.append(get_Iavg(sampling_interval)/10**-6)
            lifes.append(get_lifetime(sampling_interval,caps=caps,VH=VH)/3600)

        plt.plot(sampling_intervals,lifes,label = 'N = {}'.format(caps),color = colors[clr_ind],linestyle = lines[clr_ind])
        clr_ind += 1
    plt.plot(1,493/3600,'r*')
    plt.plot(10,5460/3600,'r*')
    plt.plot(45,24435/3600,'r*')
    plt.plot(120,18.26,'r*')
    plt.xlabel('Sampling interval $T_s$ (s)')
    plt.ylabel("$L(N,V,T_s)$ (hours)")
    plt.margins(0.0)
    plt.grid(linestyle='dotted')
    plt.legend(labelspacing = 0.05)
    plt.tight_layout() # to fit everything in the prescribed area
    plt.savefig('graphs/caps_required.pdf')
    plt.show()

if PLOT_CURRENT:
    VH = 3.3
    sampling_intervals = []
    Iavgs = []
    lifes = []

    for sampling_interval in range(1,100):
        sampling_intervals.append(sampling_interval)
        Iavgs.append(get_Iavg(sampling_interval)/10**-6)
        lifes.append(get_lifetime(sampling_interval,caps=2,VH=VH)/3600)

    plt.xlabel('Sampling interval $T_s$ (s)')
    plt.ylabel("Current ($\mu$A)")
    plt.plot(sampling_intervals,Iavgs,color = colors[3],linewidth=2)
    plt.grid(linestyle='dotted')
    plt.legend()
    plt.tight_layout() # to fit everything in the prescribed area
    plt.savefig('graphs/current_consumption.pdf')
    plt.show()

if PLOT_START_VOLTAGE:
    for caps in range(1,5):
        start_v = []
        lifes = []

        for i in range(20,34,1):
            v = i/10
            start_v.append(v)
            lifes.append(get_lifetime(sampling_interval=10,caps=caps,VH=v)/3600)
        plt.plot(start_v,lifes,label='{} capacitors'.format(caps),color = colors[clr_ind])
        clr_ind += 1
    plt.xlabel('Voltage (V)')
    plt.ylabel("Lifetime (hours)")
    plt.grid(linestyle='dotted')
    plt.legend()
    plt.margins(0.0)
    plt.tight_layout() # to fit everything in the prescribed area
    plt.savefig('graphs/start_voltage.pdf')
    plt.show()