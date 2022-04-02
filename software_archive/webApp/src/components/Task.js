import {Box, Typography} from "@mui/material";

const Task = ({task}) => {
  return (
    <Box style={{width: "auto", backgroundColor: "#f4f4f4", margin: "8px", padding: "10px 20px"}}>
      <Typography variant="h5">
        {`Device ID: ${task.DeviceId}`}
      </Typography>
      <Typography variant="h6">
        {task.Position ? `Location: ${"(" + task.Position + ")"} at Time: ${task.SampleTime}` : "No Location Data"}
      </Typography>
    </Box>
  );
};

export default Task;