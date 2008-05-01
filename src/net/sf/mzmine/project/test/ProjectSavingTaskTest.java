package net.sf.mzmine.project.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.PreloadLevel;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.StorableScan;
import net.sf.mzmine.modules.io.rawdataimport.fileformats.MzXMLReadTask;
import net.sf.mzmine.modules.peakpicking.recursivethreshold.RecursivePicker;
import net.sf.mzmine.modules.peakpicking.recursivethreshold.RecursivePickerParameters;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectManager;
import net.sf.mzmine.project.ProjectStatus;
import net.sf.mzmine.project.impl.ProjectSavingTask_xstream;
import net.sf.mzmine.project.test.comparator.FactoryComparator;
import net.sf.mzmine.taskcontrol.TaskGroup;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProjectSavingTaskTest {
	Logger logger = Logger.getLogger(this.getClass().getName());
	ProjectSavingTask_xstream savingTask;
	TestMZmineClient testClient;
	File projectDir;
	
	@Before
	public void setUp() throws Exception {
		this.testClient=TestMZmineClient.getInstance();
	}
	
	private MZmineProject setUpProject() throws Exception{
		// setup MZmineclient equivalent process
		
		MZmineProject project=MZmineCore.getCurrentProject();
		// load data file into project
		File testFilesDir=new File("resources");
		if (!testFilesDir.exists()||testFilesDir.listFiles().length<1){
			fail("No test source file available");
		}
		File sourceFile = testFilesDir.listFiles()[0];;
		boolean ok = sourceFile.exists();
		
		PreloadLevel preloadLevel = PreloadLevel.NO_PRELOAD;
		MzXMLReadTask readTask=new MzXMLReadTask(sourceFile,preloadLevel);
		Logger logger = Logger.getLogger(readTask.getClass().getName());
		logger.setLevel(Level.FINEST);
		readTask.run();

		// register some peakLists
		logger.info("Registering some peakLists");
		RecursivePicker picker = new RecursivePicker();
		picker.initModule();
		RawDataFile[] dataFiles=project.getDataFiles();
		TaskGroup taskGroup=picker.runModule(
				dataFiles, null, new RecursivePickerParameters(),null);
		while (taskGroup.getStatus()==TaskGroup.TaskGroupStatus.RUNNING){
			//wait
			Thread.sleep(1000);
			logger.info("Waiting for the peakPicking to finish");
		}
		
		return project;
	}

	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void testConsistency() throws Exception{
		MZmineProject oldProject=this.setUpProject();
		ProjectManager projectManager=this.testClient.getProjectManager();
		// create project saving task
		
		projectDir= File.createTempFile("test", "");
		projectDir.delete();
		
		logger.info("Saving project to "+projectDir.getPath());
		projectManager.saveProject(projectDir);
		
		while (projectManager.getStatus()==ProjectStatus.Processing){
			//wait
			Thread.sleep(3000);
			logger.info("Waiting for the project saving to fisnish");
		}		
		logger.info("Finished saving project");
		
		projectManager.openProject(projectDir);
		while (projectManager.getStatus()==ProjectStatus.Processing){
			//wait
			Thread.sleep(3000);
			logger.info("Waiting for the project openning to fisnish");
		}		
		logger.info("Finished opening project");		
		MZmineProject newProject=MZmineCore.getCurrentProject();
		
		OmitFieldRegistory rfRegist=new OmitFieldRegistory();
		
		rfRegist.register(StorableScan.class, "logger");
		rfRegist.register(StorableScan.class, "rawDataFile");
		
		
		FactoryComparator factoryComparator = new FactoryComparator();
		boolean ok = factoryComparator.compare(oldProject,newProject,rfRegist);
		assertTrue(ok);
	}
}