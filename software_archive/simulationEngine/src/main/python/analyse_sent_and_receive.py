import parseSimulationLog

SAMPLE_ONE_DEVICE_SENT_LOG = "./sample_one_device_simulation_log"
SAMPLE_ONE_DEVICE_RECEIVE_LOG = "./sample_one_device_receive_log"


def positions_match(pos_1, pos_2):
    threshold = 1e-9
    return abs(pos_1[0] - pos_2[0]) < threshold and abs(pos_1[1] - pos_2[1]) < threshold


class NotFoundException(Exception):
    pass


def try_search_entry(device_receive_history, sent_entry: parseSimulationLog.LogEntry, start_index):
    device_position_in_sent_history = sent_entry.position
    for i in range(start_index, len(device_receive_history)):
        device_position_in_receive_history = device_receive_history[i].position
        if positions_match(device_position_in_receive_history, device_position_in_sent_history):
            return i

    raise NotFoundException


def compute_timestamp_differences_for_one_device(device_sent_history, device_receive_history):
    """
    Notice that receive history might not be that complete.
    :param device_sent_history: [(sent_time, position)], sorted by sent_time
    :param device_receive_history: [(sent_time, position)], sorted by receive_time
    :return: [difference_in_time] for valid datas
    """
    start_index = 0
    time_differences = []
    for sent_entry in device_sent_history:
        try:
            corresponding_receive_entry_index = try_search_entry(device_receive_history,
                                                                 sent_entry,
                                                                 start_index=start_index)
            start_index = corresponding_receive_entry_index + 1
            time_difference = device_receive_history[corresponding_receive_entry_index].timestamp - sent_entry.timestamp
            time_differences.append(time_difference)

        except NotFoundException:
            pass
    return time_differences


def compute_timestamp_difference(sent_logs: parseSimulationLog.SimulationLog,
                                 receive_logs: parseSimulationLog.SimulationLog):
    timestamp_differences = {}
    for device_id, one_device_log in sent_logs.items():
        if device_id in receive_logs:
            sent_history = one_device_log.get_history()
            receive_history = receive_logs[device_id].get_history()
            timestamp_diff = compute_timestamp_differences_for_one_device(sent_history, receive_history)
            if timestamp_diff:
                timestamp_differences[device_id] = timestamp_diff
    return timestamp_differences


def analyse_logs(sent_log_filename, receive_log_filename):
    sent_log = parseSimulationLog.generate_simulation_log_from_file(sent_log_filename)
    receive_log = parseSimulationLog.generate_simulation_log_from_file(receive_log_filename)
    differences = compute_timestamp_difference(sent_log, receive_log)
    return differences


def main():
    differences = analyse_logs(SAMPLE_ONE_DEVICE_SENT_LOG, SAMPLE_ONE_DEVICE_RECEIVE_LOG)
    print(differences)


if __name__ == '__main__':
    main()
