"""
Program that handles all machine learning on ear-mounted accelerometer data.
"""
import argparse
import data_labeller
import data_ml
import matplotlib
import numpy as np
import seaborn as sns
import sys

from enum import StrEnum
from data_stats import plot_scatter_auto,plot_scatter_3d
from matplotlib import pyplot as plt
from sklearn import svm, tree
from sklearn.metrics import accuracy_score, confusion_matrix, f1_score, precision_score, recall_score, RocCurveDisplay
from sklearn.model_selection import train_test_split, permutation_test_score
from sklearn.neighbors import KNeighborsClassifier

matplotlib.rcParams.update({'font.size': 14})

class ActEM(StrEnum):
    """
    String enumeration for all possible classifications on ear-mounted accelerometer data.
    """
    earpiece_mouth_state = 'earpiece_mouth_state'
    earpiece_state_transition = 'earpiece_state_transition'

features = {
    ActEM.earpiece_mouth_state: ['x_ear', 'y_ear', 'z_ear', 'resultant'],
    ActEM.earpiece_state_transition: ['resultant_roll_stdv']
}
"""List of features per classification."""

hr_labels = {
    ActEM.earpiece_mouth_state: "Case 1",
    ActEM.earpiece_state_transition: "Case 1"
}
"""Human readable labels for all classifications."""

roc_fig = None
"""Holder for the matplotlib figure for the ROC curves display."""
roc_ax = None
"""Holder for the matplotlib axis for the ROC curves display."""
plot_feature_distr = False
"""If set to True, will plot a feature distribution plot."""
plot_confusion_matrix = False
"""Is set to True, will plot a confusion matrix for all enabled models."""
plot_decision_tree = False
"""If set to True, will plot the a visual representation of the fit decision tree."""
plot_roc_curve = False
"""If set to True, will plot ROC curves for the models."""
print_train_test_distr = False
"""If set to True, will print the distribution of labels in both the train and test set."""

plt.style.use('seaborn-v0_8-ticks')
colors = ['#000000','#E69F00','#0072B2','#56B4E9','#009E73','#F0E442','#D55E00','#CC79A7']
"""Colors to be used on the curves of the graphs."""
clr_ind = 1
"""Index to keep track of which color should be used next in the graphs."""

def plt_confusion_matrix(y_true, y_pred, title):
    """
    Plot a confusion matrix based on a list of true and predicted values.

    Parameters
    ----------
    y_true : list of str
        List of true values, to be used for confusion matrix.
    y_pred : list of str
        List of predicted values, to be used for confusion matrix.
    title : str
        Title for the confusion matrix.
    """
    # Compute confusion matrix
    labels = np.unique(y_true)
    cm = confusion_matrix(y_true, y_pred, labels=labels)
    accuracy = accuracy_score(y_true, y_pred)

    # Plot confusion matrix
    plt.figure(figsize=(8, 6))
    sns.heatmap(cm, annot=True, cmap='Blues', fmt='d', xticklabels=labels, yticklabels=labels)
    plt.xlabel('Predicted labels')
    plt.ylabel('True labels')
    plt.title(f'{title} - Accuracy:{accuracy}')

def append_labels(df):
    """
    Append mouth state (opened or closed) and mouth transition (opening and closing) labels to a given dataframe.

    Parameters
    ----------
    df : DataFrame
        The dataframe to which labels should be appended.

    Returns
    -------
    df : DataFrame
        The dataframe with labels appended. 
    """
    filtered_labels = []
    for l in df['label']:
        if l == "Mouth open":
            filtered_labels.append("Open")
        elif l == "Mouth closed":
            filtered_labels.append("Closed")
        else:
            filtered_labels.append("NA")
    df = df.assign(earpiece_mouth_state=filtered_labels)

    transition_labels = []
    for l in df['oc_transition']:
        if l == "Opening" or l == "Closing":
            transition_labels.append("Transition")
        else:
            transition_labels.append(l)
    df = df.assign(earpiece_state_transition=transition_labels)
    return df

def get_train_test_sets(labelby, filename=None, pid=None, training_features=['m_pd'], test_size=0.33, to_print=False):
    """
    Optain a dataframe based on a given classification label and filename and filter on given features. Then randomize the dataframe and split it into a train and test set with given ratio.

    Parameters
    ----------
    labelby : Act or str
        The classification label for which the train and test set should be obtained.
    filename : str, optional
        Path to the binary file from which data should be loaded. If not provided, will default to all csv files in the "labeled_data_ear_mouth" folder.
    pid : int, optional
        Identification number of the test subject to filter on. If set to any int, will only consider data from that test subject. If set to None, will consider data from all test subjects.
    training_features : list of str, default=['m_pd']
        List of features that should be included in the train and test sets. All other features will be filtered out.
    test_size : float, default=0.33
        The percentage of the dataframe that should be used for the test set.
    to_print : bool, default=False
        If set to True, will print the entire dataframe of the features and list of labels.

    Returns
    -------
    X_train : DataFrame
        Dataframe containing the features to train on.
    X_test : DataFrame
        Dataframe containing the features to test on.
    y_train : DataFrame
        Dataframe containing label to train on.
    y_test : DataFrame
        Dataframe containing labels of the test dataframe.
    label_set : Series
        Series with all unique labels in the train and test set.
    """
    # Obtain the dataframe, filter on labelby.
    df = data_labeller.get_all_data(pid, "labeled_data_ear_mouth/*.csv", data_labeller.Devices.earpiece)
    df = append_labels(df)
    df = data_labeller.append_features_to_df(df, data_labeller.Devices.earpiece)

    if labelby != None:
        df = df[df[labelby] != 'NA']
    
    # only balances if labelby is earpiece_state_transition!
    if labelby == ActEM.earpiece_state_transition:
        df = data_labeller.balance_dataframe(df, labelby)

    # Split into dataframe with only desired features and dataframe with labels.
    X = df[training_features]
    y = df[labelby]
    label_set = y.unique()
    
    if to_print:
        print(X.to_string())
        print(y.to_string())
    
    # Split into train and test sets.
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.33,random_state=42)
    return X_train, X_test, y_train, y_test, label_set

def run_decision_tree(X_train, X_test, y_train, y_test, label_set, labelby, min_samples_leaf=10, max_depth=None):
    """
    Train and run a decision tree based on a given train and test set and given hyperparameters. Also possibly plots an ROC curve and boundary display or decision tree visualization if asked to.

    Parameters
    ----------
    X_train : DataFrame
        Dataframe containing the features to train on.
    X_test : DataFrame
        Dataframe containing the features to test on.
    y_train : DataFrame
        Dataframe containing label to train on.
    y_test : DataFrame
        Dataframe containing labels of the test dataframe.
    label_set : Series
        Series with all unique labels in the train and test set.
    labelby : Act or str
        Classification on which decision tree should run.
    min_samples_leaf : int, default=10
        The minimum number of samples required to be at a leaf node.
    max_depth : int, optional
        The maximum depth of the tree. If None, then nodes are expanded until all leaves are pure or until all leaves contain less than 2 samples.

    Returns
    -------
    y_predicted : ndarray
        List of predicted values.
    y_accuracy : float
        Accuracy score of the predictions on the trained decision tree.
    """
    global clr_ind
    
    # Create a decision tree and fit to train set (train the model).
    clf = tree.DecisionTreeClassifier(random_state=42, min_samples_leaf=min_samples_leaf, max_depth=max_depth)
    clf = clf.fit(X_train, y_train)

    # Make predictions using training set.
    y_predicted = clf.predict(X_test)

    # Plot ROC curves.
    if (len(label_set) == 2 and plot_roc_curve):
        y_score = clf.predict_proba(X_test)
        RocCurveDisplay.from_predictions(y_test, y_score[:, 0], pos_label=clf.classes_[0], color=colors[clr_ind], name=f"{hr_labels[labelby]} - DT ", ax=roc_ax)
        clr_ind += 1
        
    # Calculate and print scores of predictions.
    accuracy = accuracy_score(y_test, y_predicted)
    precision = precision_score(y_test, y_predicted, labels=label_set, average='weighted')
    recall = recall_score(y_test, y_predicted, labels=label_set, average='weighted')
    f1 = f1_score(y_test, y_predicted, labels=label_set, average='weighted')

    print(f" DT  |  {accuracy:.6f}  |  {precision:.6f}  |  {recall:.6f}  |  {f1:.6f}  |")

    # If set, plot a visual representation of the decision tree.
    if plot_decision_tree:
        tree.plot_tree(clf)
        plt.show()

    return y_predicted, accuracy

def run_knn(X_train, X_test, y_train, y_test, label_set, labelby, n_neighbors=3):
    """
    Train and run a k-nearest neighbors classifier based on a given train and test set and given hyperparameters. Also possibly plots an ROC curve.

    Parameters
    ----------
    X_train : DataFrame
        Dataframe containing the features to train on.
    X_test : DataFrame
        Dataframe containing the features to test on.
    y_train : DataFrame
        Dataframe containing label to train on.
    y_test : DataFrame
        Dataframe containing labels of the test dataframe.
    label_set : Series
        Series with all unique labels in the train and test set.
    labelby : Act or str
        Classification on which classifier should run.
    n_neighbors : int, default=3
        Number of neighbors to use for prediction.

    Returns
    -------
    y_predicted : ndarray
        List of predicted values.
    y_accuracy : float
        Accuracy score of the predictions on the trained classifier.
    """
    global clr_ind

    # Create a KNN classifier and fit the training set.
    neigh = KNeighborsClassifier(n_neighbors=n_neighbors)
    clf = neigh.fit(X_train, y_train)

    # Make prediction on test set.
    y_predicted = clf.predict(X_test)

    # Plot the ROC curve if possible.
    if (len(label_set) == 2 and plot_roc_curve):
        y_score = clf.predict_proba(X_test)
        RocCurveDisplay.from_predictions(y_test, y_score[:, 0], pos_label=clf.classes_[0], color=colors[clr_ind], name=f"{hr_labels[labelby]} - KNN", ax=roc_ax)
        clr_ind += 1

    # Calculate and print prediction scores for classifier.
    accuracy = accuracy_score(y_test, y_predicted)
    precision = precision_score(y_test, y_predicted, labels=label_set, average='weighted')
    recall = recall_score(y_test, y_predicted, labels=label_set, average='weighted')
    f1 = f1_score(y_test, y_predicted, labels=label_set, average='weighted')

    print(f" KNN |  {accuracy:.6f}  |  {precision:.6f}  |  {recall:.6f}  |  {f1:.6f}  |")
    return y_predicted, accuracy

def run_svm(X_train, X_test, y_train, y_test, label_set, labelby):
    """
    Train and run a SVM classifier based on a given train and test set. Also possibly plots an ROC curve.

    Parameters
    ----------
    X_train : DataFrame
        Dataframe containing the features to train on.
    X_test : DataFrame
        Dataframe containing the features to test on.
    y_train : DataFrame
        Dataframe containing label to train on.
    y_test : DataFrame
        Dataframe containing labels of the test dataframe.
    label_set : Series
        Series with all unique labels in the train and test set.
    labelby : Act or str
        Classification on which classifier should run.

    Returns
    -------
    y_predicted : ndarray
        List of predicted values.
    y_accuracy : float
        Accuracy score of the predictions on the trained classifier.
    """
    global clr_ind

    # Create a SVM classifier and fit the training set.
    clf = svm.SVC(random_state=42, probability=True)
    clf = clf.fit(X_train, y_train)

    # Make prediction based on test set.
    y_predicted = clf.predict(X_test)

    # Plot ROC curve if possible.
    if (len(label_set) == 2 and plot_roc_curve):
        y_score = clf.predict_proba(X_test)
        RocCurveDisplay.from_predictions(y_test, y_score[:, 0], pos_label=clf.classes_[0], color=colors[clr_ind], name=f"{hr_labels[labelby]} - SVM", ax=roc_ax, plot_chance_level=True)
        clr_ind += 1

    # Calculate and print prediction scores of classifier.
    accuracy = accuracy_score(y_test, y_predicted)
    precision = precision_score(y_test, y_predicted, labels=label_set, average='weighted')
    recall = recall_score(y_test, y_predicted, labels=label_set, average='weighted')
    f1 = f1_score(y_test, y_predicted, labels=label_set, average='weighted')

    print(f" SVM |  {accuracy:.6f}  |  {precision:.6f}  |  {recall:.6f}  |  {f1:.6f}  |")
    return y_predicted, accuracy

def run_all_ml_models(labelby,
                    pid=None,
                    filename=None,
                    dt_min_samples_leaf=10,
                    dt_max_depth=None,
                    knn_n_neighbors=3,
                    enable_knn=False,
                    enable_svm=False):
    """
    Obtain a dataframe based on given parameters and run all machine learning models on a given classification with given hyperparameters.

    Parameters
    ----------
    labelby : Act or str
        Classification on which classifiers should run. Used to select features and determine if dataframe should be balanced.
    pid : int, optional
        Identification number of the test subject to filter on. If set to any int, will only consider data from that test subject. If set to None, will consider data from all test subjects.
    filename : str, optional
        Path to the binary file from which data should be loaded. If not provided, will default to all csv files in the "labeled_data_ear_mouth" folder.
    dt_min_samples_leaf : int, default=1
        Hyperparameter for DT. The minimum number of samples required to be at a leaf node.
    dt_max_depth : int, optional
        Hyperparameter for DT. The maximum depth of the tree. If None, then nodes are expanded until all leaves are pure or until all leaves contain less than 2 samples.
    knn_n_neighbors : int, default=3
        Hyperparameter for KNN. Number of neighbors to use for prediction.
    enable_knn : bool, default=False
        Is set to True, will run KNN classifier
    enable_svm : bool, default=False
        Is set to True, will run SVM classifier
    
    """
    assert labelby in features.keys(), f"Not a valid grouping {labelby}. Check labelby!"
    training_features = features[labelby]

    # Get train and test sets.
    X_train, X_test, y_train, y_test, label_set = get_train_test_sets(labelby,pid = pid,filename=filename,training_features=training_features,test_size=0.2,to_print=False)

    # If the train set is empty, return.
    if (X_train is None):
        print("Train set is empty. Skipping.")
        return
    
    # Run all models.
    print('Detecting: {}'.format(labelby))
    print(f"Model|  accuracy  | precision  |   recall   |     f1     |")
    y_predicted_dt, _ = run_decision_tree(X_train, X_test, y_train, y_test, label_set, labelby, dt_min_samples_leaf, dt_max_depth)
    if enable_knn:
        y_predicted_knn, _ = run_knn(X_train, X_test, y_train, y_test, label_set, labelby, knn_n_neighbors)
    if enable_svm:
        y_predicted_svm, _ = run_svm(X_train, X_test, y_train, y_test, label_set, labelby)

    # If enabled, plot confusion matrixes for models that ran.
    if plot_confusion_matrix:
        plt_confusion_matrix(y_test, y_predicted_dt, "DT")
        if enable_knn:
            plt_confusion_matrix(y_test, y_predicted_knn, "KNN")
        if enable_svm:
            plt_confusion_matrix(y_test, y_predicted_svm, "SVM")
        plt.show()

    # If enabled, print the train/test distribution.
    if print_train_test_distr:
        distr_train = np.unique(y_train, return_counts=True)
        distr_test = np.unique(y_test, return_counts=True)
        distr_pred = np.unique(y_predicted_dt, return_counts=True)
        print(f"Train      DT: {distr_train}")
        print(f"Test       DT: {distr_test}")
        print(f"Prediction DT: {distr_pred}")
        if len(distr_train[1]) > 1:
            print(f"Train      DT: {distr_train[1][0] / distr_train[1][1]}")
        else:
            print(f"Train      DT: {"None"}")
        if len(distr_test[1]) > 1:
            print(f"Test       DT: {distr_test[1][0] / distr_test[1][1]}")
        else:
            print(f"Test       DT: {"None"}")
        if len(distr_pred[1]) > 1:
            print(f"Prediction DT: {distr_pred[1][0] / distr_pred[1][1]}")
        else:
            print(f"Prediction DT: {"None"}")


    # If enabled, plot the feature distribution.
    if plot_feature_distr:
        if len(training_features) == 1:
            print("Only 1 feature in training set. Nothing to plot...")
            return
        if len(training_features) == 2:
            plot_scatter_auto(X_train, y_train, training_features[0], training_features[1])
        else:
            plot_scatter_3d(X_train, y_train, training_features[0], training_features[1], training_features[2])

        plt.show()

if __name__ == "__main__":
    """
    Entry point of the program. Will handle the arguments. If no arguments are set, will run only decision tree for all classifications in the groupings list with default settings.
    """
    parser = argparse.ArgumentParser(description="Program to run machine learning models on ear-mounted accelerometer generated data.")
    parser.add_argument('--knn', action='store_true',
                    help='Enables the KNN classifier.')
    parser.add_argument('--svm', action='store_true',
                    help='Enables the SVM classifier.')
    parser.add_argument('--plt_ftr_distr', action='store_true',
                    help='Plots the feature distribution.')
    parser.add_argument('--plt_conf_mtx', action='store_true',
                    help='Plots a confusion matrix for every enabled classifie.')
    parser.add_argument('--plt_dt', action='store_true',
                    help='Plots a visual representation of the decision tree classifier.')
    parser.add_argument('--plt_roc', action='store_true',
                    help='Plots ROC curves for all classifiers and classifications.')
    parser.add_argument('--prt_trn_tst_distr', action='store_true',
                    help='Prints the distribution of labels in the train and test sets.')
    run_mode = parser.add_mutually_exclusive_group()
    run_mode.add_argument('--compare', type=str, nargs=1,
                    help='Runs the given <classification> on both data from the ear-mounted accelerometer and the Densor. Provide a filename for the generate plot at COMPARE.')
    run_mode.add_argument('--pscore', action='store_true',
                    help='Runs permutation test on the given <classification> for all enabled classifiers and for all test subjects seperatly and for the combined dataframe of all test subjects.')
    parser.add_argument('classification', type=str,
                    help='The classification to run. If not set, will run both. Required if --compare or --pscore is used. Possible options are: ["state", "transition"]', nargs='?')
    
    args = parser.parse_args()
    
    groupings = [ActEM.earpiece_mouth_state, ActEM.earpiece_state_transition]

    enable_knn = args.knn
    enable_svm = args.svm

    # Handle arguments and run. If no arguments, run default settings on groupings list.
    plot_feature_distr = args.plt_ftr_distr
    plot_confusion_matrix = args.plt_conf_mtx
    plot_decision_tree = args.plt_dt
    plot_roc_curve = args.plt_roc
    print_train_test_distr = args.prt_trn_tst_distr

    compare = args.compare != None

    if args.classification == "state":
            groupings = [ActEM.earpiece_mouth_state]
    elif args.classification == "transition":
            groupings = [ActEM.earpiece_state_transition]

    if compare:
        if args.classification == None:
            print("Please provide a classification (either 'state' or 'transition') for comparisons")
            exit(1)
        
        plot_roc_curve = True
        
    elif args.pscore:
        if args.classification == None:
            print("Please provide a classification (either 'state' or 'transition') for comparisons")
            exit(1)

        labelby = groupings[0]

        print(f"PID | MD | score   | p-score")

        for pid in [2, 3, None]:
            df = data_labeller.get_all_data(pid, "labeled_data_ear_mouth/*.csv", data_labeller.Devices.earpiece)
            df = append_labels(df)
            df = data_labeller.append_features_to_df(df, data_labeller.Devices.earpiece)

            if labelby != None:
                df = df[df[labelby] != 'NA']
            
            # only balances if labelby is defined in the if-frame of this function!
            if labelby == ActEM.earpiece_state_transition:
                df = data_labeller.balance_dataframe(df, labelby)

            if (len(df) == 0):
                print("Dataframe empty. Skipping!")
                exit(0)

            x = df[features[labelby]]
            y = df[labelby]
            
            score, per_scores, p_val = permutation_test_score(tree.DecisionTreeClassifier(random_state=42, min_samples_leaf=10), x, y, random_state=42, n_jobs=-1)
            print(f" {"A" if pid == None else pid}  | {"N"}  | {score:1.5f} | {p_val:1.5f}")
        exit(0)

    if plot_roc_curve:
        roc_fig, (roc_ax) = plt.subplots(1, 1,figsize=(7.5, 2.5))
        roc_ax.grid(linestyle='dotted')
        roc_ax.margins(0.0)
        roc_fig.tight_layout(pad=1.4)

    for labelby in groupings:
        run_all_ml_models(labelby, enable_knn=enable_knn, enable_svm=enable_svm)

    if compare:
        if args.classification == "state":
            clr_ind = data_ml.run_all_ml_models_roc_feed(data_ml.Act.ed_mouth_state, roc_ax, clr_ind, dt_min_samples_leaf=10)
            clr_ind = data_ml.run_all_ml_models_roc_feed(data_ml.Act.ed_mouth_state_extended, roc_ax, clr_ind, plot_chance_level=False, dt_min_samples_leaf=10)
        elif args.classification == "transition":
            clr_ind = data_ml.run_all_ml_models_roc_feed(data_ml.Act.ed_mouth_state_transition, roc_ax, clr_ind, dt_min_samples_leaf=10)
            clr_ind = data_ml.run_all_ml_models_roc_feed(data_ml.Act.ed_mouth_state_transition_extended, roc_ax, clr_ind, plot_chance_level=False, dt_min_samples_leaf=10)

        roc_ax.set_xlabel("False positive rate")
        roc_ax.set_ylabel("True positive rate")

        filename = args.compare[0]
        save_path = './plots/{}.png'.format(filename)
        print(f"Saved to {save_path}")
        roc_fig.savefig(save_path)

    plt.show()

    