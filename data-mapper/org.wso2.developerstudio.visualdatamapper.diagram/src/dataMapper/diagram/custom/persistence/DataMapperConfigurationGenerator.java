package dataMapper.diagram.custom.persistence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.WordUtils;
import org.eclipse.emf.common.util.EList;

import dataMapper.Concat;
import dataMapper.DataMapperLink;
import dataMapper.DataMapperRoot;
import dataMapper.Element;
import dataMapper.Equal;
import dataMapper.OperatorLeftConnector;
import dataMapper.SchemaDataType;
import dataMapper.TreeNode;
import dataMapper.diagram.part.DataMapperMultiPageEditor;

public class DataMapperConfigurationGenerator {
	

    public static String generateFunction(){
    	
    	List<JsFunction> dmcFunctions = new ArrayList<JsFunction>();
    	
    	DataMapperRoot rootDiagram =  (DataMapperRoot) DataMapperMultiPageEditor.getGraphicalEditor().getDiagram().getElement();
    	TreeNode inputTreeNode = rootDiagram.getInput().getTreeNode().get(0);
    	TreeNode outputTreeNode = rootDiagram.getOutput().getTreeNode().get(0);

    	String input = inputTreeNode.getName();
		String output = outputTreeNode.getName();
		
		String functionStart = "function map_S_"+input+"_S_"+output+"(" + input + ", " + output + "){\n";
		String functionReturn = "return " + output + ";\n";
		
		JsFunction mainFunction = new JsFunction(0);
		mainFunction.setFunctionStart(functionStart);
		mainFunction.setFunctionReturn(functionReturn);
		dmcFunctions.add(mainFunction);
		
		List<JsFunction> innerFunctions = getFunctionForTheTreeNode(rootDiagram.getInput().getTreeNode(), dmcFunctions, 0);
		mainFunction.getFunctions().addAll(innerFunctions);
		
		String documentString = "";
		for (JsFunction func : dmcFunctions) {
			documentString += func.toString() + "\n\n";
		}
		
		return documentString;
    }
    

    private static List<JsFunction> getFunctionForTheTreeNode(EList<TreeNode> eList, List<JsFunction> allFunctions, int indentation){
    	indentation++;
    	Iterator<TreeNode> treeNodeIterator =  eList.iterator();
    	List<JsFunction> mappingFunctions = new ArrayList<JsFunction>();
		while(treeNodeIterator.hasNext()){
			TreeNode node = treeNodeIterator.next();
			JsFunction func = getOnetoOneFunction(node, allFunctions, indentation);
			if (func != null && func.getAssignmentStatements().size() > 0) {
				mappingFunctions.add(func);
			}
    	} 	
    	return mappingFunctions;
    }

	

	private static JsFunction getOnetoOneFunction(TreeNode treeNode, List<JsFunction> allFunctions, int indentation) {
		
		JsFunction forLoopFunction = new JsFunction(indentation);
		String collectionName = getInputSuperparentName(treeNode);
		String indexer = getIndexFromIndent(indentation);
		String functionStart = "";
		boolean recursive = false;
			
		if (treeNode.getSchemaDataType().equals(SchemaDataType.ARRAY)) {
			functionStart = "for (var " + indexer + " in " + collectionName+") {\n";
			recursive = true;
		} else {
			forLoopFunction.setFunctionEnd("");
		}
		
		forLoopFunction.setFunctionStart(functionStart);
		
		JsFunction one2OnemapingFunction = null;
		
		//for element
		Iterator<Element> elementIterator = treeNode.getElement().iterator();
		while(elementIterator.hasNext()){
			
			Element inputElement = elementIterator.next();
			EList<DataMapperLink> elementLinkList = inputElement.getOutNode().getOutgoingLink();
			
			Iterator<DataMapperLink> mapperLinkIterator = elementLinkList.iterator();
			while(mapperLinkIterator.hasNext()){
				DataMapperLink mapperLinkObject = mapperLinkIterator.next();
				if(mapperLinkObject.getInNode() != null){
					//element -----> element
					Element outputElement = mapperLinkObject.getInNode().getElementParent();					
					if(outputElement != null){
						
						if (one2OnemapingFunction == null) { // we want this to happen only in one time
							one2OnemapingFunction = new JsFunction(0);
							String outputChild = outputElement.getName();
							String inputChild = inputElement.getName();
							String outputParent = getOutputSuperparentName(outputElement.getFieldParent());
							String inputParent = getInputSuperparentName(inputElement.getFieldParent());
							
							String outputImmediateParent = getOutputParentName(outputElement);
							String inputImmediateParent = getInputParentName(inputElement);
							
							String inputFunctionName = inputImmediateParent + WordUtils.capitalize(inputChild);
							String outputFunctionName = outputImmediateParent + WordUtils.capitalize(outputChild);
							
							if (recursive) {
								String indexerWithBrackets = "["+ indexer +"]";
								inputParent = inputParent.replace("." + inputChild, "") + indexerWithBrackets;
								outputParent = outputParent.replace("." + outputChild, "") + indexerWithBrackets;
								String innerFunctionStart = "function map_L_"+inputImmediateParent+"_L_"+outputImmediateParent+"(" + inputImmediateParent + ", " + outputImmediateParent + "){\n";
								one2OnemapingFunction.setFunctionStart(innerFunctionStart);
								String functionCallStatement = "\tmap_L_"+inputImmediateParent+"_L_"+outputImmediateParent+"(" + inputParent + ", " + outputParent + ");\n";
								forLoopFunction.getAssignmentStatements().add(functionCallStatement);
							} else {
								String innerFunctionStart = "function map_S_"+inputFunctionName+"_S_"+outputFunctionName+"(" + inputImmediateParent + ", " + outputImmediateParent + "){\n";
								one2OnemapingFunction.setFunctionStart(innerFunctionStart);
								String functionCallStatement = "\tmap_S_"+inputFunctionName+"_S_"+outputFunctionName+"(" + inputParent + ", " + outputParent + ");\n";
								forLoopFunction.getAssignmentStatements().add(functionCallStatement);
							}	
						}
						
						String mapStatement = getOnetoOneMappingStatement(inputElement, outputElement);
						one2OnemapingFunction.getAssignmentStatements().add(mapStatement);
					}	
				}
			}
			
		}
	
		List<JsFunction> l2Function = getFunctionForTheTreeNode(treeNode.getNode(), allFunctions, indentation);
		forLoopFunction.getFunctions().addAll(l2Function);

		if (one2OnemapingFunction != null) {
			allFunctions.add(one2OnemapingFunction);
		}
		
		return forLoopFunction;
	}
	


	private static String getIndexFromIndent(int indentation) {
		char indexChar = 'i';
		for (int i=0; i< indentation; i++) {
			indexChar ++;
		}
		return Character.toString(indexChar);
	}


	private static String getOnetoOneMappingStatement(Element inputElement, Element outputElement) {
		String outputParent = getOutputParentName(outputElement);
		String outputChild = outputElement.getName();
		String inputParent = getInputParentName(inputElement);
		String inputChild = inputElement.getName();
		String statement = "\t" + outputParent +"." + outputChild + "=" + inputParent + "."+ inputChild +";\n";
		return statement;
	}
    
    private static String getInputSuperparentName(TreeNode node){
    	String paramName = "";
    	if (node.getFieldParent() != null) {
    		paramName = node.getName();
        	while(node.getFieldParent() != null){
        		String name = node.getFieldParent().getName();
        		paramName = name + "." + paramName;
        		node = node.getFieldParent();
        	}
    	} else {
    		paramName = node.getName();
    	}

		return paramName;
    }
    
    private static String getOutputSuperparentName(TreeNode node){
    	String paramName = "";
    	if (node.getFieldParent() != null) {
    		paramName = node.getName();
        	while(node.getFieldParent() != null){
        		String name = node.getFieldParent().getName();
        		paramName = name + "." + paramName;
        		node = node.getFieldParent();
        	}
    	} else {
    		paramName = node.getName();
    	}
    	
		return paramName;
    }
    
    private static String getInputParentName(Element node){
    	String temp = "input";
    	if(node.getFieldParent() != null){
    		temp = node.getFieldParent().getName();
    	}
		return temp;
    }
    
    private static String getOutputParentName(Element node){
    	String temp = "output";
    	if(node.getFieldParent() != null){
    		temp = node.getFieldParent().getName();
    	}
		return temp;
    }
    
    
	private static String getInputElementForLeft(OperatorLeftConnector leftConnector) {
		return leftConnector.getInNode().getIncomingLink().get(0).getOutNode().getElementParent()
				.getName();

	}

	private static String getOutputElementForRight(Equal equal) {
		return equal.getBasicContainer().getRightContainer().getRightConnectors().get(0)
				.getOutNode().getOutgoingLink().get(0).getInNode().getElementParent().getName();
	}

	private static String getOutputElementForRight(Concat concat) {
		return concat.getBasicContainer().getRightContainer().getRightConnectors().get(0)
				.getOutNode().getOutgoingLink().get(0).getInNode().getElementParent().getName();
	}
    
 
}