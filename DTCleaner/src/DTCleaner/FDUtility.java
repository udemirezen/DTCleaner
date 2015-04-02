package DTCleaner;
import weka.core.Utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import weka.core.Instances;


/**
 * Used to return two objects in returnViolatedTuples
 */
class violatedTuples {
	public final Instances instances;
	public final HashMap<Integer, List<String>> tupleID;
	
	public violatedTuples(Instances instances, HashMap<Integer, List<String>> tupleID) {
		this.instances = instances;
		this.tupleID = tupleID;
	}
}

/**
 * Functional Dependency Utilities
 * 
 */
public class FDUtility {

	
	public static void addFD(Instances i){
		System.out.println("Add new FD. Usage: attribute#->attribute#,attribute# \n e.g. \"1->2,3\"\n");
		System.out.println("# Name\n===============");
		for(int j = 0; j < i.numAttributes();j++) System.out.println(j + " " + i.attribute(j).name());
		
		Scanner sc = new Scanner(System.in);
		String fd = sc.next(); // the functional dependency 
		
	}

	/**
	 * Loads functional dependencies from file.
	 * Syntax of file:
	 * 	1->2,3,4
	 *  5->7
	 *  ...
	 * @param filename, e.g "data/FDslist.txt"
	 * @return HashMap of FDs
	 * @throws FileNotFoundException 
	 */
	public static HashMap<String, String[]> readFDs(String filename) throws FileNotFoundException{
		HashMap<String, String[]> FDs = new HashMap<String, String[]>();
		
		System.out.println("\nReading FDs: " + filename + "...\n");
		Scanner in = new Scanner(new FileReader(filename));
		
		while(in.hasNextLine()){
			String line = in.nextLine();
			if(line.contains(",")){
				String [] fd = line.split("->");
				String key = fd[0];
				String [] rhs = fd[1].split(",");
				FDs.put(key, rhs);
			}else if(!line.substring(line.indexOf('>')+1, line.length()).contains(",")){
				String key = line.substring(0, line.indexOf('-'));
				String[] rhs = {line.substring(line.indexOf('>')+1,line.length())};
				FDs.put(key, rhs);
			}else{
				// invalid FD syntax
				System.out.println("invalid FD syntax: "+ line);
				continue;
			}
		}
		
		
		return FDs;
	}
	
	/**
	 * Prints a summary of FDs
	 * @param FDs
	 * @param i
	 * @return String summary: summary of FDs
	 */
	public static String toSummaryString(Instances i,HashMap<String, String[]> FDs){
		StringBuilder summary = new StringBuilder();
		summary.append("Num FDs: "+FDs.keySet().size()+"\n\n");
		
		int counter = 1;
		
		for(String premise : FDs.keySet()){
			StringBuilder fd = new StringBuilder();
			fd.append(Utils.padLeft("" + (counter++), 4)+" ");
			fd.append(i.attribute(Integer.parseInt(premise)).name());
			fd.append(" -> ");
			for(String RHS : FDs.get(premise)){
				fd.append(i.attribute(Integer.parseInt(RHS)).name()+", ");
			}


			//remove the last ", "
			fd.deleteCharAt(fd.length()-1);
			fd.deleteCharAt(fd.length()-1);
			
			
			//add to summary
			summary.append(fd.toString() + "\n");
		}
		
		return summary.toString();
	}
	

	/**
	 * Check whether the dataset satisfies all FDs
	 * @param i: The dataset instances
	 * @param FDs: list of FDs
	 * @return true if dataset satisfies all FDs, otherwise false.
	 */
	public static boolean checkFDSatisfiaction(Instances i,	HashMap<String, String[]> FDs) {		
		HashMap<List<Object>,Object> map = new HashMap<List<Object>,Object>();
		System.out.println("\nChecking FD sataisfactian...\n");
		for(String premiseID : FDs.keySet()){
			String [] rhsIDs = FDs.get(premiseID);
			for(int j = 0; j < i.numInstances(); j++){
				List<Object> rhsValues = new LinkedList<Object>();
				for(int k = 0; k < rhsIDs.length; k++) rhsValues.add(i.instance(j).toString(Integer.parseInt(rhsIDs[k])));
				String premise = i.instance(j).toString(Integer.parseInt(premiseID));
				if(map.containsKey(rhsValues) && !map.get(rhsValues).equals(premise)){
					
					System.out.println("The following pair violate an FD:");
					System.out.println(rhsValues.toString() + " " + premise);
					System.out.println(rhsValues.toString() + " " + map.get(rhsValues) + "\n");
					
					return false;
				}
				else{
					map.put(Collections.unmodifiableList(rhsValues), premise);
				}
			}
		}
		return true;
	}
	
	/**
	 * Finds and returns a list of tuples that violates the FDs
	 * @param i
	 * @param FDs
	 * @return v, tupleIDs: Violated instances in weka instances format, and a list of tupleIDs and their FDs that they violate
	 */
	
	//needs to be fixed
	public static violatedTuples returnViolatedTuples(Instances i, HashMap<String, String[]> FDs){
		Instances v = new Instances(i,0);
		
		//Holds RHS values in List<Object> and the premise and the tuple index in SImpleImmutableEntry<String,Integer>
		HashMap<List<Object>,SimpleImmutableEntry<String,Integer>> map = new HashMap<List<Object>,SimpleImmutableEntry<String,Integer>>();
		//Holds tuple index of violated tuples, and the FD it violates
		HashMap<Integer, List<String>> tupleID = new HashMap<Integer, List<String>>();
		
		HashMap<String, SimpleImmutableEntry<String,Integer>> premises = new HashMap<String, SimpleImmutableEntry<String,Integer>>();
		
		System.out.println("\nFinding violated tuples...\n");
		
		for(String premiseID : FDs.keySet()){
			String [] rhsIDs = FDs.get(premiseID);
			
			String fd = premiseID+"->";
			String rhs = "";
			for(String r : rhsIDs) rhs = r + ",";
			rhs = rhs.substring(0, rhs.length()-1); // delete last ','
			fd = fd+rhs; // merge
			fd = FDtoString(i, fd);
			
			for(int j = 0; j < i.numInstances(); j++){
				List<Object> rhsValues = new LinkedList<Object>();
				for(int k = 0; k < rhsIDs.length; k++) rhsValues.add(i.instance(j).toString(Integer.parseInt(rhsIDs[k])));
				String premise = i.instance(j).toString(Integer.parseInt(premiseID));
				if(map.containsKey(rhsValues) && !map.get(rhsValues).getKey().contentEquals(premise)){
					int tupleIndex = map.get(rhsValues).getValue();

					// Add index to list of violated tuples
					// Add the tuple to the list of violated tuples

					if(!tupleID.containsKey(tupleIndex)){
						List<String> vFDs = new LinkedList<String>();
						vFDs.add(fd);
						tupleID.put(tupleIndex, vFDs);
						v.add(i.instance(tupleIndex));
					}
					if(!tupleID.containsKey(j)){
						List<String> vFDs = new LinkedList<String>();
						vFDs.add(fd);
						tupleID.put(j,vFDs);
						v.add(i.instance(j));
					}

				}else if(!map.containsKey(rhsValues) && premises.keySet().contains(premise)){
					if(!tupleID.containsKey(premises.get(premise).getValue())){
						List<String> vFDs = new LinkedList<String>();
						vFDs.add(fd);
						tupleID.put(j,vFDs);
						v.add(i.instance(j));
						v.add(i.instance(premises.get(premise).getValue()));
					}
					
				}else{
					map.put(Collections.unmodifiableList(rhsValues), new SimpleImmutableEntry<String,Integer>(premise,j));
					premises.put(premise, new SimpleImmutableEntry<String,Integer>(premise,j));
				}
			}
		}
	
		System.out.println("Found: "+ v.numInstances() + " violating tuples.");
		
		System.out.println(v);
		violatedTuples pair = new violatedTuples(v, tupleID);
		return pair;
		
	}
	
	/**
	 * Returns the FD by it's name, e.g. given  1->2, return "HospitalName->Address1"
	 * @param i, premiseToRHS
	 * @return
	 */
	public static String FDtoString(Instances i, String FDIDs){
		String [] fd = FDIDs.split("->");
		String premise = fd[0];
		String rhs = fd[1];
		
		StringBuilder result = new StringBuilder();
		result.append(i.attribute(Integer.parseInt(premise)).name());
		result.append("->");
		if(rhs.contains(",")){
			String[] rhsList = rhs.split(",");
			for(String r : rhsList) result.append(i.attribute(Integer.parseInt(r)).name()+", ");
			//remove the last ", "
			result.deleteCharAt(result.length()-1);
			result.deleteCharAt(result.length()-1);
			
		}else{
			 result.append(i.attribute(Integer.parseInt(rhs)).name());
		}
		
		return result.toString();
		
	}

}
