import collections

from src.main.python import test_util_generate_logs
from analyse_sent_and_receive import analyse_logs
import matplotlib.pyplot as plt


def get_time_diff():
    sent_log_filename = test_util_generate_logs.GENERATED_SENT_LOGS
    receive_log_filename = test_util_generate_logs.GENERATED_RECEIVE_LOGS
    result = analyse_logs(sent_log_filename, receive_log_filename)  # dict of lists
    return result


def filter_noise(diff):
    return diff


def generate_visualisations(diff):
    diff_lists = diff.values()

    # histogram
    plt.figure(1)
    flat_diff = [time for diffs in diff_lists for time in diffs]
    plt.hist(flat_diff)

    # valid data points
    plt.figure(2)
    valid_positions = [len(l) for l in diff_lists]
    frequencies = collections.Counter(valid_positions)
    plt.plot(frequencies.keys(), frequencies.values())

    plt.show()


def main():
    diff = get_time_diff()
    filtered_diff = filter_noise(diff)
    generate_visualisations(filtered_diff)


if __name__ == '__main__':
    main()
