"""
Script that holds methods for plotting machine learning related plots.
"""
from matplotlib import pyplot as plt
import numpy as np

from data_labeller import *

def plot_scatter(X, y, l1, l2):
    """
    Creates a scatter plot with x=photodiode readings and y=temperature readings, on two given labels.

    Parameters
    ----------
    X : DataFrame
        Dataset containing features and labels.
    y : str
        Column name of the classification column from which labels should be used.
    l1 : str
        First filter to include in scatter plot.
    l2 : str
        Second filter to include in scatter plot.
    """
    plt.scatter(X[y==l1]['m_pd'], X[y==l1]['m_temp'], label=l1)
    plt.scatter(X[y==l2]['m_pd'], X[y==l2]['m_temp'], label=l2)
    plt.xlabel('Light intensity')
    plt.ylabel('Temperature')
    plt.legend()
    plt.show()

def plot_scatter_auto(X, y, a1='m_pd', a2='m_temp'):
    """
    Automatically creates a 2D-scatter plot with given features for axis, on all given labels.

    Parameters
    ----------
    X : DataFrame
        Dataframe containing features.
    y : Dataframe
        Dataframe containing labels of all samples.
    a1 : str, default="m_pd"
        Feature name that should be used for the x-axis of the scatter plot.
    a2 : str, default="m_temp"
        Feature name that should be used for the y-axis of the scatter plot.
    """
    labels = np.unique(y)
    for l in labels:
        plt.scatter(X[y==l][a1], X[y==l][a2], label = l)
    plt.xlabel(a1)
    plt.ylabel(a2)
    plt.legend()
    plt.show()

def plot_scatter_3d(X, y, a1='m_accel_x', a2 ='m_accel_y', a3 ='m_accel_z'):
    """
    Automatically creates a 3D-scatter plot with given features for axis, on all given labels.

    Parameters
    ----------
    X : DataFrame
        Dataframe containing features.
    y : Dataframe
        Dataframe containing labels of all samples.
    a1 : str, default="m_accel_x"
        Feature name that should be used for the x-axis of the scatter plot.
    a2 : str, default="m_accel_y"
        Feature name that should be used for the y-axis of the scatter plot.
    a3 : str, default="m_accel_z"
        Feature name that should be used for the y-axis of the scatter plot.
    """
    fig = plt.figure()
    ax = fig.add_subplot(projection='3d')
    labels = np.unique(y)
    for l in labels:
        ax.scatter(X[y==l][a1],X[y==l][a2],X[y==l][a3],label = l)
    ax.set_xlabel(a1)
    ax.set_ylabel(a2)
    ax.set_zlabel(a3)
    plt.legend()
    plt.show()

def plot_box_mouth_state(filename=None):
    """
    Takes a .csv filename from labelled data and plots box-plot containing photodiode reading distribution per label.

    Parameters
    ----------
    filename : str, optional
        Path to the csv file that should be used for the plot.
    """
    if filename == None:
        df = get_all_data_with_labels()
    else:
        df = get_file_data_with_labels(filename)

    mouth_state = df[df['mouth_state'] != 'NA']
    mouth_state = mouth_state[['m_pd','mouth_state']]
    ax = mouth_state.boxplot( by="mouth_state", figsize=(10, 8))
    plt.show()

def plot_wearing_box(df):
    """
    Plots a box plot of the distribution of temperature readings per label in a given dataframe. Saves the plot in 'plots/inuse_box.pdf'

    Parameters
    ----------
    df : DataFrame
        The dataframe to use for the box plot.
    """
    plt.style.use('seaborn-v0_8-ticks')
    plt.rcParams.update({'font.size': 15})
    mouth_state = df[df['wear_state'] != 'NA']
    mouth_state = mouth_state[['m_temp','wear_state']]
    ax = mouth_state.boxplot( by="wear_state", figsize=(6, 3),grid = False,boxprops= dict(linewidth=3.0, color='black'),whiskerprops=dict(linestyle='-',linewidth=3.0, color='black'))
    ax.set_xlabel('')
    ax.set_ylabel('Temperature (C)')
    ax.yaxis.grid(True)

    plt.title('')
    plt.suptitle('') 
    plt.tight_layout()
    plt.savefig('plots/inuse_box.pdf')
    plt.show()

def plot_mouthopen_box(df):
    """
    Plots a box plot of the distribution of photodiode readings per label in a given dataframe. Saves the plot in 'plots/mouth_box.pdf'

    Parameters
    ----------
    df : DataFrame
        The dataframe to use for the box plot.
    """
    plt.style.use('seaborn-v0_8-ticks')
    plt.rcParams.update({'font.size': 15})
    mouth_state = df[df['mouth_state'] != 'NA']
    mouth_state = mouth_state.replace("Open", "Mouth opened")
    mouth_state = mouth_state.replace("Closed", "Mouth closed")
    mouth_state = mouth_state[['m_pd','mouth_state']]
    ax = mouth_state.boxplot(by="mouth_state", figsize=(6, 3), grid=False, boxprops=dict(linewidth=3.0, color='black'), whiskerprops=dict(linestyle='-', linewidth=3.0, color='black'))
    ax.set_xlabel('')
    ax.set_ylabel('Light intensity')
    ax.yaxis.grid(True)

    plt.title('')
    plt.suptitle('') 
    plt.tight_layout()
    plt.savefig('plots/mouth_box.pdf')
    plt.show()

def plot_box_by_group(filename=None, labelby=None, columns=None):
    """
    Takes a classification and list of features and creates a box plot showing the distribution of the columns per label of the classification.
    
    Parameters
    ----------
    filename : str, optional
        Path to a csv file to be loaded. Its data is used for the box plot. If not provided, will load all csv files in the 'labeled_data' folder.
    labelby : str, optional
        The classification for which the box plot should be created.
    columns : list of str
        List of column names which should be included in the box plot.
    """
    if filename == None:
        df = get_all_data_with_labels(labelby=labelby)
    else:
        df = get_file_data_with_labels(filename, labelby=labelby)

    if columns != None:
        columns.append(labelby)
        df = df[columns]
    df.boxplot(by=labelby, figsize=(10, 8))
    plt.show()

def plot_box(filename = None):
    """
    Possibly takes a filename and creates a box plot of the dataframe created from the file.
    
    Parameters
    ----------
    filename : str, optional
        Path to a csv file to be loaded. Its data is used for the box plot. If not provided, will load all csv files in the 'labeled_data' folder.
    """
    if filename == None:
        df = get_all_data_with_labels()
    else:
        df = get_file_data_with_labels(filename)

    df.boxplot(figsize=(10, 8))
    plt.show()

def plot_temperature_trace(filename = None):
    """
    Possibly takes a filename and plots the temperature readings of the dataframe created from the file.
    
    Parameters
    ----------
    filename : str, optional
        Path to a csv file to be loaded. Its data is used for the plot. If not provided, will load all csv files in the 'labeled_data' folder.
    """
    df = get_file_data_with_labels(filename)
    df['m_temp'][20:].plot()
    plt.show()
