import matplotlib
import matplotlib.pyplot as plt
import pandas as pd
import sys
import numpy as np

date_fmt = '%d-%m-%y %H:%M:%S'

plt.style.use('seaborn-v0_8-ticks')
colors = ['#000000','#FF0000','#56B4E9','#009E73','#F0E442','#0072B2','#D55E00','#CC79A7']
clr_ind = 1
linewidth = 3

matplotlib.rcParams.update({'font.size': 16})

df = pd.read_csv("./labeled_data_ear_mouth/2024-04-24-p2-e-1.csv")
df_d = pd.read_csv("./labeled_data/2024-04-24-p2-e-1.csv")
print(df)
x = df["ts"]
x = [float(i) for i in x]
dump_start = 1713972832
df_d.insert(0, 'ts', range(dump_start, dump_start + len(df_d)))

labels = df["label"]

fig,(ax) = plt.subplots(figsize=(5,2.5))
fig.tight_layout(pad = 1.7)

data = df.drop("ts", axis=1)
data = data.drop("label", axis=1)

for c_name, c in data.items():
    clr_ind += 1

tick_labels = []
ticks = []

for i, l in enumerate(labels):
    if i == 0 or labels.iloc[i - 1] != l:
        tick_labels.append(l)
        ticks.append(df["ts"].iloc[i])

clr_ind = 1
df["res"] = np.sqrt(df["x_ear"]**2 + df["y_ear"]**2 + df["z_ear"]**2)
df_d["m_accel_x"] = df_d["m_accel_x"] / 16384
df_d["m_accel_y"] = df_d["m_accel_y"] / 16384
df_d["m_accel_z"] = df_d["m_accel_z"] / 16384
df_d["res"] = np.sqrt(df_d["m_accel_x"]**2 + df_d["m_accel_y"]**2 + df_d["m_accel_z"]**2)
fx = []
fy = []
fy_x = []
fy_y = []
fy_z = []
fx_d = []
fy_d_imu = []
fy_x_imu = []
fy_y_imu = []
fy_z_imu = []
fy_d_pd = []

ts_min = 1713972945
ts_max = 1713972955

for i, ts in enumerate(x):
    if ts >= ts_min and ts <= ts_max:
        fx.append(ts - ts_min)
        fy.append(df["res"].iloc[i])
        fy_x.append(df["x_ear"].iloc[i])
        fy_y.append(df["y_ear"].iloc[i])
        fy_z.append(df["z_ear"].iloc[i])
for i, ts in enumerate(df_d["ts"]):
    if ts >= ts_min and ts <= ts_max:
        fx_d.append(ts - ts_min)
        fy_d_imu.append(df_d["res"].iloc[i])
        fy_x_imu.append(df_d["m_accel_x"].iloc[i])
        fy_y_imu.append(df_d["m_accel_y"].iloc[i])
        fy_z_imu.append(df_d["m_accel_z"].iloc[i])
        fy_d_pd.append(df_d["m_pd"].iloc[i])

name = ""

if (sys.argv[1] == "--ear"):
    name="ear"
    ax.set_xlabel("Time (s)")
    ax.set_ylabel("Acceleration (g)")

    ax.plot(fx, fy_x, color=colors[clr_ind], label="x")
    clr_ind += 1
    ax.plot(fx, fy_y, color=colors[clr_ind], label="y")
    clr_ind += 1
    ax.plot(fx, fy_z, color=colors[clr_ind], label="z")
    ax.set_yticks([0.0, 0.5, 1.0])
elif (sys.argv[1] == "--mimu"):
    name="mouth_imu"
    ax.set_xlabel("Time (s)")
    ax.set_ylabel("Acceleration (g)")
    
    ax.plot(fx_d, fy_x_imu, color=colors[clr_ind], label="x")
    clr_ind += 1
    ax.plot(fx_d, fy_y_imu, color=colors[clr_ind], label="y")
    clr_ind += 1
    ax.plot(fx_d, fy_z_imu, color=colors[clr_ind], label="z")
    ax.set_yticks([0.0, 0.5, 1.0])
else:
    name="mouth_pd"
    ax.set_xlabel("Time (s)")
    ax.set_ylabel("Light intensity")

    ax.plot(fx_d, fy_d_pd, color=colors[clr_ind], label="intraoral light")

ax.legend(loc='upper center', bbox_to_anchor=(0.5, -0.35), ncol=3)
plt.savefig(f"./plots/ear_mouth_cls_event_{name}.png", format="png", bbox_inches="tight")
plt.show()

