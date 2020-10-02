This is a REST web-service that allows passing and executing javascript code. 
To explore and try out API use the following link:
http://localhost:8080/swagger-doc

###### **METRICS** 

The app exposes its performance metrics through which you can see how it works at runtime.
Also, it provides services like prometheus and grafana to convenient observing and testing. 

**Setup**

Before observing the metrics do the following:

1. Open the terminal in the project folder and run gradle task 'jibDockerBuilder' to build app image.
2. Run command 'docker-compose up' to launch app along with prometheus and grafana servers.

**Metrics for observing**

* js_executor:request_count:rate (5m rate of http request except '/actuator/prometheus' endpoint);
* map_size;
* js_executor:map_size:avg (5m average of map_size);
* script_number (number of scripts in the map by status);
* js_executor:script_number:avg (5m average of script_number);
* running_time_seconds_count (running time counter);
* running_time_seconds_sum (total running time);
* js_executor:total_running_time:rate (5m rate of running_time_seconds_sum);
* running_time_mean (mean running time);
* running_time_seconds_max (max running time);
* running_time_seconds{quantile="0.0"} (min running time);

Other available metrics you can find via the link: http://localhost:8080/actuator/prometheus

**Prometheus**

To testing metrics through prometheus follow http://localhost:9090/.
Then type any of the metrics listed above in the provided field.

**Grafana**

To observing the listed metrics through grafana follow the steps below:

1. Navigate to http://localhost:3000.
2. Use 'admin' as login and password.
3. Write the new password.
4. On the left panel find settings and navigate to 'Data source'.
5. Click 'add data source' and select prometheus.
6. Write 'prometheus:9090' in the 'URL' field  and click 'Save & Test'. You will see 'Data source is working'.
7. On the left panel find the add tab, navigate to 'Import', click 'Upload JSON file'
  and select the file named 'grafana-dashboard.json'. You will see the dashboard.
8. In the left top corner select time needed.



