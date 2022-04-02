import Task from './Task'
import {Box, Paper, Typography} from "@mui/material";

const Tasks = ({tasks, id}) => {
  return (
    <Box style={{padding: "8px", width: "100%", height: "70vh"}}>
      <Paper elevation={3} style={{width: '100%', padding: "8px", height: "100%", overflowY: "auto"}}>
        <Typography gutterBottom align="center" variant="h4">
          {`History for ${id}:`}
        </Typography>
        {
          typeof tasks !== "undefined" ?
          tasks.map((task, index) => (
            <Task
              key={index}
              task={task}
            />
          )) : <></>
        }
      </Paper>
    </Box>
  );
};

export default Tasks;