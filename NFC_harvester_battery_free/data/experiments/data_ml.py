"""
Program that handles all machine learning on Densor data.
"""
import argparse
import matplotlib
import seaborn as sns
import numpy as np

from data_labeller import get_all_data_with_labels, get_file_data_with_labels, get_time_series_df, balance_dataframe
from data_stats import plot_scatter_3d, plot_scatter_auto, plot_mouthopen_box, plot_wearing_box
from enum import StrEnum
from matplotlib import pyplot as plt
from sklearn import svm, tree
from sklearn.inspection import DecisionBoundaryDisplay
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, RocCurveDisplay, confusion_matrix
from sklearn.model_selection import permutation_test_score, train_test_split
from sklearn.neighbors import KNeighborsClassifier

matplotlib.rcParams.update({'font.size': 14})

class Act(StrEnum):
    """
    String enumeration for all possible classifications on densor data.
    """
    sleep_position = "sleep_position"
    mouth_state = "mouth_state"
    wear_state = "wear_state"
    laying_state = "laying_state"
    sleep_state = "sleep_state"
    awake_asleep = "awake_asleep"
    speaking_state = "speaking_state"
    drinking_state = "drinking_state"
    ed_mouth_state = "ed_mouth_state"
    ed_mouth_state_extended = "ed_mouth_state_extended"
    ed_mouth_state_transition = "ed_mouth_state_transition"
    ed_mouth_state_transition_extended = "ed_mouth_state_transition_extended"
    grinding_state = "grinding_state"
    grinding_state_extended = "grinding_state_extended" # Also includes speaking as possibility
    awake_asleep_ts = "awake_asleep_ts"
    mouth_state_combined = "mouth_state_combined"

hr_labels = {
    Act.sleep_position: "Densor - Sleep position",
    Act.mouth_state: "Densor - Mouth opening state",
    Act.wear_state: "Densor - Orthodontic compliance",
    Act.laying_state: "Densor - Laying state",
    Act.sleep_state: "Densor - Sleep state",
    Act.awake_asleep: "Densor - Awake or asleep",
    Act.speaking_state: "Densor - Speaking detection",
    Act.drinking_state: "Densor - Fluid intake detection",
    Act.ed_mouth_state: "Case 2",
    Act.ed_mouth_state_extended: "Case 3",
    Act.ed_mouth_state_transition: "Case 2",
    Act.ed_mouth_state_transition_extended: "Case 3",
    Act.grinding_state: "Densor - Grinding state",
    Act.grinding_state_extended: "Densor - Ginding state extended",
    Act.awake_asleep_ts: "Densor - Awake or asleep extended",
    Act.mouth_state_combined: "Densor - Mouth state, ear mouth and regular data"
}
"""
Human readable labels for all classifications.
"""
    

features = {
    Act.sleep_position: ['m_accel_x','m_accel_y','m_accel_z','resultant'],
    Act.mouth_state: ['m_pd'],
    Act.wear_state: ['m_temp'],
    Act.laying_state: ['m_temp','m_accel_x','m_accel_y','m_accel_z','resultant'],
    Act.sleep_state: ['m_temp','m_accel_x','m_accel_y','m_accel_z','resultant_abs'],
    Act.awake_asleep: ['m_temp','resultant'],
    Act.speaking_state:['pd_var'],
    Act.drinking_state:['temp_diff_min'],
    Act.ed_mouth_state: ['m_accel_x', 'm_accel_y', 'm_accel_z', 'resultant'],
    Act.ed_mouth_state_extended: ['m_accel_x', 'm_accel_y', 'm_accel_z', 'resultant', 'm_pd'],
    Act.ed_mouth_state_transition: ['resultant_roll_stdv'],
    Act.ed_mouth_state_transition_extended: ['pd_roll_stdv', 'resultant_roll_stdv'],
    Act.grinding_state: ['resultant_var'],
    Act.grinding_state_extended: ['resultant_var'],
    Act.awake_asleep_ts: ['resultant_var', 'temp_diff_min'],
    Act.mouth_state_combined: ['m_pd']
}
"""
List of features per classification.
"""

db_display_features = {
    Act.sleep_position: ['m_accel_y','resultant'],
    Act.mouth_state: ['m_pd'],
    Act.wear_state: ['m_temp'],
    Act.laying_state: ['m_temp', 'resultant'],
    Act.sleep_state: ['m_temp','resultant_abs'],
    Act.awake_asleep: ['m_temp','resultant'],
    Act.speaking_state:['pd_var'],
    Act.drinking_state:['temp_diff_min'],
    Act.ed_mouth_state: ['m_accel_z', 'resultant'],
    Act.ed_mouth_state_extended: ['resultant', 'm_pd'],
    Act.ed_mouth_state_transition: ['resultant_roll_stdv'],
    Act.ed_mouth_state_transition_extended: ['pd_roll_stdv', 'resultant_roll_stdv'],
    Act.grinding_state: ['resultant_var'],
    Act.grinding_state_extended: ['resultant_var'],
    Act.awake_asleep_ts: ['resultant_var', 'temp_diff_min'],
    Act.mouth_state_combined: ['m_pd']
}
"""
List of features that should be displayed on the decision boundary plots.
"""

time_series_based = [Act.speaking_state, Act.drinking_state, Act.grinding_state, Act.grinding_state_extended, Act.awake_asleep_ts]
"""List of classifications that are time-series based. Will use a different set of features then other classifications."""
balance = [Act.sleep_state, Act.awake_asleep, Act.ed_mouth_state_transition, Act.wear_state, Act.ed_mouth_state_transition_extended, Act.laying_state]
"""List of classifications for which the dataframe should be balanced before training and testing."""

roc_fig = None
"""Holder for the matplotlib figure for the ROC curves display."""
roc_ax = None
"""Holder for the matplotlib axis for the ROC curves display."""
optimizing = False
"""Boolean to tell if the system is optimizing. Will adapt the ROC curve plotting based on this."""
plot_feature_distr = False
"""If set to True, will plot a feature distribution plot."""
plot_boundaries = False
"""If set to True, will plot a visual representation of the decision boundaries for the decision tree."""
plot_confusion_matrix = False
"""Is set to True, will plot a confusion matrix for all enabled models."""
plot_decision_tree = False
"""If set to True, will plot the a visual representation of the fit decision tree."""
plot_box_plots = False
"""If set to True, will plot box plots for train class distribution when running mouth_state or wear_state classification."""
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
    sns.heatmap(cm, annot=True, cmap="Blues", fmt="d", xticklabels=labels, yticklabels=labels)
    plt.xlabel('Predicted labels')
    plt.ylabel('True labels')
    plt.title(f'{title} - Accuracy:{accuracy}')
    

def get_train_test_sets(labelby, filename=None, pid=None, training_features=['m_pd'], test_size=0.33, to_print=False):
    """
    Optain a dataframe based on a given classification label and filename and filter on given features. Then randomize the dataframe and split it into a train and test set with given ratio.

    Parameters
    ----------
    labelby : Act or str
        The classification label for which the train and test set should be obtained.
    filename : str, optional
        Path to the binary file from which data should be loaded. If not provided, will default to all csv files in the "labeled_data" folder.
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
    # Obtain the dataframe.
    if filename == None:
        if labelby not in time_series_based:
            df = get_all_data_with_labels(labelby=labelby, pid=pid)
        else:
            df = get_all_data_with_labels(pid=pid)
    else:
        df = get_file_data_with_labels(filename=filename, labelby=labelby)

    # Convert to time-series based if necessary.
    if labelby in time_series_based:
        df = get_time_series_df(df, labelby)

    if (len(df) == 0):
        print("Dataframe empty. Skipping!")
        return None, None, None, None, None

    # Only balances if labelby is defined in the balance list.
    if labelby in balance:
        balanceby = labelby
        df = balance_dataframe(df, balanceby)
    
    # Split into dataframe with only desired features and dataframe with labels.
    X = df[training_features]
    y = df[labelby]
    label_set = y.unique()
    
    if to_print:
        print(X.to_string())
        print(y.to_string())
    
    # Split into train and test sets.
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=test_size, random_state=42)
    return X_train, X_test, y_train, y_test, label_set


def run_decision_tree(X_train, X_test, y_train, y_test, label_set, labelby, plot_chance_level=False, min_samples_leaf=1, max_depth=None):
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
    plot_chance_level : bool, default=False
        If set to True, will plot the chance level on the ROC axis.
    min_samples_leaf : int, default=1
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
        if optimizing:
            # Plot with i as label, used for optimization.
            RocCurveDisplay.from_predictions(y_test, y_score[:, 0], pos_label=clf.classes_[0], name=f"{i} - DT ", ax=roc_ax, plot_chance_level=plot_chance_level)
        else:
            # Plot with human readable classification labels.
            RocCurveDisplay.from_predictions(y_test, y_score[:, 0], pos_label=clf.classes_[0], color=colors[clr_ind], name=f"{hr_labels[labelby]} - DT ", ax=roc_ax, plot_chance_level=plot_chance_level)
        
        clr_ind += 1

    # If set, plot visual representation of decision boundaries.
    if plot_boundaries:
        display_features = X_train[db_display_features[labelby]]
        if (len(db_display_features[labelby]) == 2):
            
            pred_colors_idx = [""] + y_train.unique().tolist()
            pred_colors = [colors[pred_colors_idx.index(l)] for l in y_train]

            dbd = DecisionBoundaryDisplay.from_estimator(clf, display_features, response_method="predict", xlabel=display_features.columns[0], ylabel=display_features.columns[1], alpha=0.5)
            dbd.ax_.scatter(display_features.iloc[:, 0], display_features.iloc[:, 1], c=pred_colors, edgecolor="k")
            plt.show()

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

def run_knn(X_train, X_test, y_train, y_test, label_set, labelby, plot_chance_level=False, n_neighbors=3):
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
    plot_chance_level : bool, default=False
        If set to True, will plot the chance level on the ROC axis.
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
        if optimizing:
            # Plot with i as label, used for optimization.
            RocCurveDisplay.from_predictions(y_test, y_score[:, 0], pos_label=clf.classes_[0], name=f"{i} - KNN", ax=roc_ax, plot_chance_level=plot_chance_level)
        else:
            # Plot with human readable classification labels.
            RocCurveDisplay.from_predictions(y_test, y_score[:, 0], pos_label=clf.classes_[0], color=colors[clr_ind], name=f"{hr_labels[labelby]} - KNN", ax=roc_ax, plot_chance_level=plot_chance_level)

    # Calculate and print prediction scores for classifier.
    accuracy = accuracy_score(y_test, y_predicted)
    precision = precision_score(y_test, y_predicted, labels=label_set, average='weighted')
    recall = recall_score(y_test, y_predicted, labels=label_set, average='weighted')
    f1 = f1_score(y_test, y_predicted, labels=label_set, average='weighted')

    print(f" KNN |  {accuracy:.6f}  |  {precision:.6f}  |  {recall:.6f}  |  {f1:.6f}  |")
    return y_predicted, accuracy

def run_svm(X_train, X_test, y_train, y_test, label_set, labelby, plot_chance_level=False):
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
    plot_chance_level : bool, default=False
        If set to True, will plot the chance level on the ROC axis.

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
        if optimizing:
            # Plot with i as label, used for optimization.
            RocCurveDisplay.from_predictions(y_test, y_score[:, 0], pos_label=clf.classes_[0], name=f"{i} - SVM", ax=roc_ax, plot_chance_level=plot_chance_level)
        else:
            # Plot with human readable classification labels.
            RocCurveDisplay.from_predictions(y_test, y_score[:, 0], pos_label=clf.classes_[0], color=colors[clr_ind], name=f"{hr_labels[labelby]} - SVM", ax=roc_ax, plot_chance_level=plot_chance_level)


    # Calculate and print prediction scores of classifier.
    accuracy = accuracy_score(y_test, y_predicted)
    precision = precision_score(y_test, y_predicted, labels=label_set, average='weighted')
    recall = recall_score(y_test, y_predicted, labels=label_set, average='weighted')
    f1 = f1_score(y_test, y_predicted, labels=label_set, average='weighted')

    print(f" SVM |  {accuracy:.6f}  |  {precision:.6f}  |  {recall:.6f}  |  {f1:.6f}  |")
    return y_predicted, accuracy

def run_all_ml_models(
                    labelby,
                    pid=None,
                    filename=None,
                    plot_chance_level=False,
                    dt_min_samples_leaf=1,
                    dt_max_depth=None,
                    knn_n_neighbors=3,
                    enable_knn=False,
                    enable_svm=False
    ):
    """
    Obtain a dataframe based on given parameters and run all machine learning models on a given classification with given hyperparameters.

    Parameters
    ----------
    labelby : Act or str
        Classification on which classifiers should run. Used to select features and determine if dataframe should be balanced.
    pid : int, optional
        Identification number of the test subject to filter on. If set to any int, will only consider data from that test subject. If set to None, will consider data from all test subjects.
    filename : str, optional
        Path to the binary file from which data should be loaded. If not provided, will default to all csv files in the "labeled_data" folder.
    plot_chance_level : bool, default=False
        If set to True, will plot the chance level on the ROC axis on the last model that runs.
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
    X_train, X_test, y_train, y_test, label_set = get_train_test_sets(labelby,pid = pid,filename=filename,training_features=training_features,test_size=0.33,to_print=False)

    # Determine which model should plot the chance level on the ROC curve, if it needs to be plotted at all.
    plt_cl_dt = (not enable_knn) and (not enable_svm) and plot_chance_level
    plt_cl_knn = enable_knn and (not enable_svm) and plot_chance_level
    plt_cl_svm = enable_svm and plot_chance_level

    # If the train set is empty, return.
    if (X_train is None):
        print("Train set is empty. Skipping.")
        return

    # Run all models.
    print('Detecting: {}'.format(labelby))
    print(f"Model|  accuracy  | precision  |   recall   |     f1     |")
    y_predicted_dt, _ = run_decision_tree(X_train, X_test, y_train, y_test, label_set, labelby, plot_chance_level=plt_cl_dt, min_samples_leaf=dt_min_samples_leaf, max_depth=dt_max_depth)
    if enable_knn:
        y_predicted_knn, _ = run_knn(X_train, X_test, y_train, y_test, label_set, labelby, plt_cl_knn, knn_n_neighbors)
    if enable_svm:
        y_predicted_svm, _ = run_svm(X_train, X_test, y_train, y_test, label_set, labelby, plt_cl_svm)
    

    # If enabled, plot confusion matrixes for models that ran.
    if plot_confusion_matrix:
        plt_confusion_matrix(y_test,y_predicted_dt, "DT")
        if enable_knn:
            plt_confusion_matrix(y_test,y_predicted_knn, "KNN")
        if enable_svm:
            plt_confusion_matrix(y_test,y_predicted_svm, "SVM")
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

    # If enabled and running either wear_state or mouth_state classification, plot the box plot for class distribution.
    if plot_box_plots:
        if labelby == Act.wear_state:
            plot_wearing_box(get_all_data_with_labels(pid = pid))
        elif labelby == Act.mouth_state or labelby == Act.mouth_state_combined:
            new_df = get_all_data_with_labels(pid=pid)
            new_df = new_df[new_df["pid"] != 1]
            plot_mouthopen_box(new_df)
        
        plt.show()
                
def run_all_ml_models_roc_feed(labelby, ax, clr_idx, pid = None, filename = None, plot_chance_level=False, dt_min_samples_leaf=1, dt_max_depth=None, knn_n_neighbors=3, enable_knn=False, enable_svm=False):
    """
    Obtain a dataframe based on given parameters and run all machine learning models on a given classification with given hyperparameters. Plot ROC curves on a given axis.

    Parameters
    ----------
    labelby : Act or str
        Classification on which classifiers should run. Used to select features and determine if dataframe should be balanced.
    ax : matplotlib axes
        The matplotlib axis to plot the ROC curve on.
    clr_idx : ind
        Color index, used to select the color to use for the curve.
    pid : int, optional
        Identification number of the test subject to filter on. If set to any int, will only consider data from that test subject. If set to None, will consider data from all test subjects.
    filename : str, optional
        Path to the binary file from which data should be loaded. If not provided, will default to all csv files in the "labeled_data" folder.
    plot_chance_level : bool, default=False
        If set to True, will plot the chance level on the ROC axis on the last model that runs.
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
    
    Returns
    -------
    clr_ind : int
        Color index, used to select the color to use for plotting a curve.
    """
    global roc_ax
    global clr_ind
    global plot_roc_curve

    roc_ax = ax
    clr_ind = clr_idx
    plot_roc_curve = True

    run_all_ml_models(labelby, pid, filename, plot_chance_level, dt_min_samples_leaf=dt_min_samples_leaf, dt_max_depth=dt_max_depth, knn_n_neighbors=knn_n_neighbors, enable_knn=enable_knn, enable_svm=enable_svm)

    return clr_ind

if __name__ == "__main__":
    """
    Entry point of the program. Will handle the arguments. If no arguments are set, will run only decision tree for all classifications in the groupings list with default settings.
    """
    parser = argparse.ArgumentParser(description="Program to run machine learning models on Densor generated data.")
    parser.add_argument('--knn', action='store_true',
                    help='Enables the KNN classifier.')
    parser.add_argument('--svm', action='store_true',
                    help='Enables the SVM classifier.')
    parser.add_argument('--plt_ftr_distr', action='store_true',
                    help='Plots the feature distribution.')
    parser.add_argument('--plt_bnd_disp', action='store_true',
                    help='Plots a visual representation of boundaries of the decision tree classifier.')
    parser.add_argument('--plt_conf_mtx', action='store_true',
                    help='Plots a confusion matrix for every enabled classifie.')
    parser.add_argument('--plt_dt', action='store_true',
                    help='Plots a visual representation of the decision tree classifier.')
    parser.add_argument('--plt_roc', action='store_true',
                    help='Plots ROC curves for all classifiers and classifications.')
    parser.add_argument('--prt_trn_tst_distr', action='store_true',
                    help='Prints the distribution of labels in the train and test sets.')
    parser.add_argument('--plt_box_plt', action='store_true',
                    help='Plots box plots with the photodiode or temperature readings distributions per label when running mouth_state or wear_state classification respectively.')
    run_mode = parser.add_mutually_exclusive_group()
    run_mode.add_argument('--optimize', type=int, nargs=2,
                    help='Runs the decision tree classifier with max depth in range (min, max) on <classification>. Min should be at least 1.')
    run_mode.add_argument('--loopall', action='store_true',
                    help='Runs the given <classification> on all enabled classifiers for all test subjects seperatly and for the combined dataframe of all test subjects.')
    run_mode.add_argument('--pscore', action='store_true',
                    help='Runs permutation test on the given <classification> for all enabled classifiers and for all test subjects seperatly and for the combined dataframe of all test subjects.')
    parser.add_argument('classification', type=str,
                    help='The classification to run. If not set, will run [wear_state, mouth_state, speaking_state, drinking_state]. Required if --optimize, --loopall or --pscore is used.', nargs='?')
    
    args = parser.parse_args()

    groupings = [Act.wear_state, Act.mouth_state, Act.speaking_state, Act.drinking_state]

    enable_knn = args.knn
    enable_svm = args.svm

    # Handle arguments and run. If no arguments, run default settings on groupings list.
    plot_feature_distr = args.plt_ftr_distr
    plot_boundaries = args.plt_bnd_disp
    plot_confusion_matrix = args.plt_conf_mtx
    plot_decision_tree = args.plt_dt
    print_train_test_distr = args.prt_trn_tst_distr
    plot_box_plots = args.plt_box_plt

    if args.plt_roc:
        # Create a new matplotlib instance for the ROC curves.
        roc_fig, roc_ax = plt.subplots(figsize=(16,8))
        plot_roc_curve = True

    if args.optimize != None:
        optimizing = True
        roc_fig, roc_ax = plt.subplots(figsize=(16,8))
        plot_roc_curve = True

        enable_knn = False
        enable_svm = False

        if args.classification == None:
            print("Classification needs to be provided for --optimize, but none was provided! Exiting...")

        for i in range(args.optimize[0], args.optimize[1]):
            print(f"{i:03} ", end="")

            run_all_ml_models(args.classification, dt_min_samples_leaf=i, enable_knn=enable_knn, enable_svm=enable_svm)
            
        plt.show()
        exit(0)

    elif args.loopall:
        if args.classification == None:
            print("Classification needs to be provided for --optimize, but none was provided! Exiting...")

        for i in range(1, 4):
            print(f"\r\n{i:03} ")
                
            labelby = args.classification

            run_all_ml_models(labelby, pid=i, enable_knn=enable_knn, enable_svm=enable_svm)
            print(f"\r\n{i:03} DT Max Depth=1")
            run_all_ml_models(labelby, pid=i, dt_max_depth=1, enable_knn=enable_knn, enable_svm=enable_svm)

        print(f"\r\nAll ")
        i = "All"

        run_all_ml_models(labelby, enable_knn=enable_knn, enable_svm=enable_svm)
        print(f"\r\nAll DT Max Depth=1")
        run_all_ml_models(labelby, dt_max_depth=1, enable_knn=enable_knn, enable_svm=enable_svm)
            
        plt.show()
        exit(0)

    elif args.pscore:
        if args.classification == None:
            print("Classification needs to be provided for --optimize, but none was provided! Exiting...")

        labelby = args.classification

        print(f"Model | PID | MD | score   | p-score")

        for pid in [1, 2, 3, None]:
            if labelby not in time_series_based:
                df = get_all_data_with_labels(labelby=labelby,pid = pid)
            else:
                df = get_all_data_with_labels(pid = pid)

            if (len(df) == 0):
                print("Dataframe empty. Skipping!")
                continue

            if labelby in time_series_based:
                df = get_time_series_df(df, labelby)
    
            x = df[features[labelby]]
            y = df[labelby]
            
            for md in [None, 1]:
                score, per_scores, p_val = permutation_test_score(tree.DecisionTreeClassifier(random_state=42, max_depth=md), x, y, random_state=42)
                print(f" DT   |  {"A" if pid == None else pid}  | {"N" if md == None else md}  | {score:1.5f} | {p_val:1.5f}")

            if enable_knn:
                score, per_scores, p_val = permutation_test_score(KNeighborsClassifier(n_neighbors=3), x, y, random_state=42)
                print(f" KNN  |  {"A" if pid == None else pid}  | -  | {score:1.5f} | {p_val:1.5f}")
            if enable_svm:
                score, per_scores, p_val = permutation_test_score(svm.SVC(random_state=42, probability=True), x, y, random_state=42)
                print(f" SVM  |  {"A" if pid == None else pid}  | -  | {score:1.5f} | {p_val:1.5f}")
        
        exit(0)
    else:
        if args.classification != None:
            groupings = [args.classification]

    for labelby in groupings:
        run_all_ml_models(labelby, enable_knn=enable_knn, enable_svm=enable_svm)
        plt.show()
