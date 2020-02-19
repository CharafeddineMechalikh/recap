# RECAP-DES
RECAP Discrete Event Simulation Framework an extension for CloudSimPlus (http://cloudsimplus.org/)

# Installation
1. Clone the repo on your local disk
2. Download "Ecllipse Java Eclipse IDE for Enterprise Java Developers" or any other IDE with Maven support. Also make sure you have Java JDK version 1.8 and higher installed.
3. Import the clonned repo "RecapSim-package" via Maven. In Eclipse IDE go to "File -> Import -> Maven -> Existing Maven Projects". This will add the project to your workspace and download any dependencies needed.

# Usage
1. Command Line (CLI) mode
	* Under the "RecapSimPlus" component you can use "RecapSimPlusLauncher.java" to run CLI commands. It supports two major options staring experiment by using simulation model file and test mode to create a dummy model  on the fly and run it
	* Test mode: eu.recap.sim.RecapSimPlusLauncher -test -s2 -n2 -a2 -c2 -d2 -r2 -l5 -o0
	* Expeiment file: eu.recap.sim.RecapSimPlusLauncher -e ReadyModels\ElasticSearchExperiment.sim
	* If using Eclipse, see example launching options the "RecapSimPlusLauncherExperiment" and "RecapSimPlusLauncherTestMode" under the "Java Application -> Run Configurations" menu.

2. Using Web API
	* Under the "RecapSim-API" component launch main class "RecapSimApiService" which will start an Apache web service on your machine where you can post simulation experiments in JSON format
	* For example "curl -X POST -H "Content-Type: application/json" -d @../ReadyModels/ElasticSearchExperiment.json http://localhost:4567/StartSimulation"

3. Use a pre-made usecase scenario generator
	* Under the "RecapSim-usecases" component you can launch "LinknovateValidationLauncher" main class file that will: 
		1. Generate simulation models
		2. Save models to files .sim and .json ( you can look at JSON file to see the model structure)
		3. Run the generated experiment via simulation


# Useful files to start with
You can run these files in debug mode to see the step by step logic of model creation and simulation start to understant how things work.
	
* eu.recap.sim.RecapSimPlusLauncher.java
* eu.recap.sim.RecapSimPlusLauncher.java
* eu.recap.sim.RecapSim.java
* eu.recap.sim.usecases.validation.linknovate.LinknovateValidationLauncher.java

# Structure

Component  | Description
------------- | -------------
RecapSim-package  | Root package that contains all the folders below.
RecapSim-API  | REST API component that can be used to launch experiments over the web.
RecapSim-models  | Models that are used to create simulation experiments. Protobuffer files plus the generated Java code. See https://developers.google.com/protocol-buffers for more detail.
RecapSimPlus  | Core code and extensions for CloudSimPlus library.
RecapSim-usecases | An example code that used to generate a simulation experiment based on an Elastic Search application usage logs. See "Modelling and Simulation of ElasticSearch using CloudSim" publication at https://ieeexplore.ieee.org/abstract/document/8958653 for which this was used as a basis.



