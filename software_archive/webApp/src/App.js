import Map from "./components/Map";
import Tasks from "./components/Tasks";
import {useEffect, useState} from "react";
import {Box, CssBaseline, Typography} from "@mui/material";
import axios from "axios";
const util = require('util')

function App() {
  const [tasks, setTasks] = useState({});

  const trackersIds = useState({ids: []})

  useEffect(function () {
    async function fetchTasksFromTracker(id) {

      // Do not request again if the data has been previously requested
      if (tasks[id]) {
        return
      }
      console.log('Request Data at Time: ', new Date().getTime());
      const response = await axios.get(`https://xvojnsbpvd.execute-api.eu-west-1.amazonaws.com/default/getLocation?type=list&trackername=${id}`);
      if (typeof response.data.Entries === "undefined") {
        trackersIds[1](prevState => ({
          ids: prevState.ids.filter(cId => cId !== id)
        }))
        return
      }
      console.log('Fetch Data at Time: ', new Date().getTime());
      console.log('Got Data: ', util.inspect(response.data.Entries));

      // Update the data
      setTasks(prevState => ({
        ...prevState,
        [id]: response.data.Entries
      }));
    }

    // Display a list of devices and the corresponding current positions for all added trackers
    async function fetchAllTasks() {
      for (const id of trackersIds[0].ids) {
        await fetchTasksFromTracker(id);
      }
    }

    fetchAllTasks()
  }, [trackersIds, tasks])

  return (<>
    <CssBaseline/>
    <Box style={{width: '90%', padding: "4px", margin: '16px 5% 16px 5%'}}>
      <Typography variant="h2" align="center"> High Throughput Computing </Typography>
      <Map trackerIds={trackersIds}/>
      {trackersIds[0].ids.length > 0 ?
        trackersIds[0].ids.map((id, i) => {
          return <Tasks key={i} tasks={tasks[id]} id={id}/>
        }) : <></>
      }
    </Box>
  </>);
}

export default App;
