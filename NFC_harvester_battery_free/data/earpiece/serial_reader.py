import struct
import matplotlib.pyplot as plt
import serial
import sys
import csv
import threading

from datetime import datetime

x_len = 1190
y_lim = 5

x1_d = []
x2_d = []
y1_d = []
y2_d = []
z1_d = []
z2_d = []
x_s1 = []
x_s2 = []
sample_count = 0
lock = threading.Lock()

def read_thread_legacy():
    global x1_d
    global x2_d
    global y1_d
    global y2_d
    global z1_d
    global z2_d
    global x_s1
    global x_s2
    global sample_count
    global lock

    s1 = serial.Serial(sys.argv[1], baudrate=9600, timeout=10)
    s2 = serial.Serial(sys.argv[2], baudrate=9600, timeout=10)
    s1.readline()
    s1.readline()

    while sample_count <= x_len:
        r1 = s1.read(14)
        r2 = s2.read(14)

        # print(r1)
        if (sample_count % 10 == 0):
            print(sample_count)
        

        lock.acquire()
        try:
            # print("s1", r1)
            val_x = struct.unpack('f', r1[0:4])
            val_y = struct.unpack('f', r1[4:8])
            val_z = struct.unpack('f', r1[8:12])

            x_s1.append(sample_count)
            x1_d.append(val_x)
            y1_d.append(val_y)
            z1_d.append(val_z)
        except Exception as e:
            print(e)
            print(f"s1: tried to parse incorrect value: {r1}. Ignoring!")

        try:
            # print("s2", r2)
            val_x = struct.unpack('f', r2[0:4])
            val_y = struct.unpack('f', r2[4:8])
            val_z = struct.unpack('f', r2[8:12])
        
            x_s2.append(sample_count)
            x2_d.append(val_x)
            y2_d.append(val_y)
            z2_d.append(val_z)
        except Exception as e:
            print(e)
            print(f"s2: tried to parse incorrect value: {r2}. Ignoring!")

        sample_count = sample_count + 1
        lock.release()

def read_thread_single_chann():
    global x1_d
    global y1_d
    global z1_d
    global x_s1
    global sample_count
    global lock

    s1 = serial.Serial(sys.argv[1], baudrate=115200, timeout=10)
    s1.reset_input_buffer()
    s1.readline()

    while True:
        r1 = s1.read(8)
        curr = datetime.now().timestamp()

        lock.acquire()
        try:
            val_x = int.from_bytes(r1[0:2], byteorder='little',signed = True) / 16384
            val_y = int.from_bytes(r1[2:4], byteorder='little',signed = True) / 16384
            val_z = int.from_bytes(r1[4:6], byteorder='little',signed = True) / 16384

            x_s1.append(curr)
            x1_d.append(val_x)
            y1_d.append(val_y)
            z1_d.append(val_z)
        except Exception as e:
            print(e)
            print(f"s1: tried to parse incorrect value: {r1}. Ignoring!")

        sample_count = sample_count + 1
        lock.release()

def read_thread_dual_chann():
    global x1_d
    global x2_d
    global y1_d
    global y2_d
    global z1_d
    global z2_d
    global x_s1
    global x_s2
    global sample_count
    global lock

    s1 = serial.Serial(sys.argv[1], baudrate=115200, timeout=10)
    s2 = serial.Serial(sys.argv[2], baudrate=115200, timeout=10)
    s1.reset_input_buffer()
    s2.reset_input_buffer()
    s1.readline()
    s2.readline()

    while True:
        r1 = s1.read(8)
        r2 = s2.read(8)
        curr = datetime.now().timestamp()

        lock.acquire()
        try:
            val_x = int.from_bytes(r1[0:2], byteorder='little',signed = True) / 16384
            val_y = int.from_bytes(r1[2:4], byteorder='little',signed = True) / 16384
            val_z = int.from_bytes(r1[4:6], byteorder='little',signed = True) / 16384

            x_s1.append(curr)
            x1_d.append(val_x)
            y1_d.append(val_y)
            z1_d.append(val_z)
        except Exception as e:
            print(e)
            print(f"s1: tried to parse incorrect value: {r1}. Ignoring!")

        try:
            val_x = int.from_bytes(r2[0:2], byteorder='little',signed = True) / 16384
            val_y = int.from_bytes(r2[2:4], byteorder='little',signed = True) / 16384
            val_z = int.from_bytes(r2[4:6], byteorder='little',signed = True) / 16384

            x_s2.append(curr)
            x2_d.append(val_x)
            y2_d.append(val_y)
            z2_d.append(val_z)
        except Exception as e:
            print(e)
            print(f"s2: tried to parse incorrect value: {r2}. Ignoring!")

        sample_count = sample_count + 1
        lock.release()

def main_legacy():
    global lock

    print(sys.argv[1])
    print(sys.argv[2])

    serial_thread = threading.Thread(target=read_thread_legacy)
    serial_thread.start()

    plt.ion()
    fig, axs = plt.subplots(3)
    axs[0].set_title("x-axis")
    axs[1].set_title("y-axis")
    axs[2].set_title("z-axis")

    prev_sample_count = 0

    while (sample_count <= x_len):
        # print(f"s1 length: {len(x_s1)}")
        # print(f"s2 length: {len(x_s2)}")
        # print(f"sample_count: {sample_count}")

        axs[0].cla()
        axs[1].cla()
        axs[2].cla()

        lock.acquire()
        axs[0].plot(x_s1[-x_len:],x1_d[-x_len:], label=sys.argv[1])
        axs[0].plot(x_s2[-x_len:],x2_d[-x_len:], label=sys.argv[2])
        axs[1].plot(x_s1[-x_len:],y1_d[-x_len:], label=sys.argv[1])
        axs[1].plot(x_s2[-x_len:],y2_d[-x_len:], label=sys.argv[2])
        axs[2].plot(x_s1[-x_len:],z1_d[-x_len:], label=sys.argv[1])
        axs[2].plot(x_s2[-x_len:],z2_d[-x_len:], label=sys.argv[2])
        lock.release()

        axs[0].set_ylim(-1 * y_lim, y_lim)
        axs[1].set_ylim(-1 * y_lim, y_lim)
        axs[2].set_ylim(-1 * y_lim, y_lim)

        axs[0].legend()
        axs[1].legend()
        axs[2].legend()

        fig.canvas.draw()
        fig.canvas.flush_events()
        # plt.draw()

        new_samples = sample_count - prev_sample_count
        curr_sample_count = sample_count

        prev_sample_count == curr_sample_count
            

    with open(sys.argv[3],'w') as f:
            writer = csv.writer(f)
            for i in range(x_len):
                # print(f"writing {i}")
                writer.writerow([i, x1_d[i], x2_d[i], y1_d[i], y2_d[i], z1_d[i], z2_d[i]])

    plt.show(block=True)

def main_single_chann():
    global lock

    print(sys.argv[1])

    with open(sys.argv[2],'w') as f:
        writer = csv.writer(f)
        writer.writerow(["ts", "x_ear", "y_ear", "z_ear"])

    serial_thread = threading.Thread(target=read_thread_single_chann)
    serial_thread.start()

    # plt.ion()
    # fig, axs = plt.subplots(3)
    # axs[0].set_title("x-axis")
    # axs[1].set_title("y-axis")
    # axs[2].set_title("z-axis")

    prev_sample_count = 0

    while True:

        # axs[0].cla()
        # axs[1].cla()
        # axs[2].cla()

        lock.acquire()
        # axs[0].plot(x_s1[-x_len:],x1_d[-x_len:], label=sys.argv[1])
        # axs[1].plot(x_s1[-x_len:],y1_d[-x_len:], label=sys.argv[1])
        # axs[2].plot(x_s1[-x_len:],z1_d[-x_len:], label=sys.argv[1])

        curr_sample_count = sample_count

        lock.release()

        # axs[0].set_ylim(-1 * 1, 1)
        # axs[1].set_ylim(-1 * 1, 1)
        # axs[2].set_ylim(-1 * 1, 1)

        # axs[0].legend()
        # axs[1].legend()
        # axs[2].legend()

        # fig.canvas.draw()
        # fig.canvas.flush_events()
            

        with open(sys.argv[2],'a') as f:
                writer = csv.writer(f)
                for i in range(prev_sample_count, curr_sample_count):
                    print(f"ts: {x_s1[i]}, x: {x1_d[i]:3.2}, y: {y1_d[i]:3.2}, z: {z1_d[i]:3.2}")
                    writer.writerow([x_s1[i], x1_d[i], y1_d[i], z1_d[i]])

        prev_sample_count = curr_sample_count

    # plt.show(block=True)

def main_dual_chann():
    global lock

    print(sys.argv[1])
    print(sys.argv[2])

    with open(sys.argv[3],'w') as f:
        writer = csv.writer(f)
        writer.writerow(["ts", "x_ear", "y_ear", "z_ear", "x_mouth", "y_mouth", "z_mouth"])

    serial_thread = threading.Thread(target=read_thread_dual_chann)
    serial_thread.start()

    plt.ion()
    fig, axs = plt.subplots(3)
    axs[0].set_title("x-axis")
    axs[1].set_title("y-axis")
    axs[2].set_title("z-axis")

    prev_sample_count = 0

    while True:

        axs[0].cla()
        axs[1].cla()
        axs[2].cla()

        lock.acquire()
        axs[0].plot(x_s1[-x_len:],x1_d[-x_len:], label="ear")
        axs[0].plot(x_s2[-x_len:],x2_d[-x_len:], label="mouth")
        axs[1].plot(x_s1[-x_len:],y1_d[-x_len:], label="ear")
        axs[1].plot(x_s2[-x_len:],y2_d[-x_len:], label="mouth")
        axs[2].plot(x_s1[-x_len:],z1_d[-x_len:], label="ear")
        axs[2].plot(x_s2[-x_len:],z2_d[-x_len:], label="mouth")

        curr_sample_count = sample_count

        lock.release()

        axs[0].set_ylim(-1 * 1, 1)
        axs[1].set_ylim(-1 * 1, 1)
        axs[2].set_ylim(-1 * 1, 1)

        axs[0].legend()
        axs[1].legend()
        axs[2].legend()

        fig.canvas.draw()
        fig.canvas.flush_events()
            

        with open(sys.argv[3],'a') as f:
                writer = csv.writer(f)
                for i in range(prev_sample_count, curr_sample_count):
                    print(f"ts: {x_s1[i]}, x_e: {x1_d[i]:3.2}, y_e: {y1_d[i]:3.2}, z_e: {z1_d[i]:3.2}, x_m: {x2_d[i]:3.2}, y_m: {y2_d[i]:3.2}, z_m: {z2_d[i]:3.2}")
                    writer.writerow([x_s1[i], x1_d[i], y1_d[i], z1_d[i], x2_d[i], y2_d[i], z2_d[i]])

        prev_sample_count = curr_sample_count

    plt.show(block=True)

if __name__ == "__main__":
    # Provide as: python serial_reader.py <ear_path> <output_path>
    if len(sys.argv) == 3:
        main_single_chann()
    # Provide as: python serial_reader.py <ear_path> <mouth_path> <output_path>
    else:
        main_dual_chann()