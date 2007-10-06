/*
 * Created on 04.06.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.jopdesign.build;


import java.util.*;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.CPInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.util.InstructionFinder;

/**
 * Find a correct order of static class initializers (<clinit>)
 * @author martin
 *
 */
public class ClinitOrder extends MyVisitor {


	Map clinit = new HashMap();
	
	public ClinitOrder(JOPizer jz) {
		super(jz);
	}
		
	public void visitJavaClass(JavaClass clazz) {
		super.visitJavaClass(clazz);
		MethodInfo mi = cli.getMethodInfo(JOPizer.clinitSig);
		if (mi!=null) {
			Set depends = findDependencies(mi.getMethod());
			clinit.put(cli, depends);
		}
	}	
	
	/**
	 * Print the dependency for debugging. Not used at the moment.
	 *
	 */
	private void printDependency() {

		Set cliSet = clinit.keySet();
		Iterator itCliSet = cliSet.iterator();
		while (itCliSet.hasNext()) {
		
			ClassInfo clinf = (ClassInfo) itCliSet.next();
			System.out.println("Class "+clinf.clazz.getClassName());
			Set depends = (Set) clinit.get(clinf);
				
			Iterator it = depends.iterator();
			while(it.hasNext()) {
				ClassInfo clf = (ClassInfo) it.next();
				System.out.println("\tdepends "+clf.clazz.getClassName());
			}
		}
	}
	/**
	 * Find a 'correct' oder for the static <clinit>.
	 * Throws an error on cyclic dependencies.
	 * 
	 * @return the ordered list of classes
	 */
	public List findOrder() {

		Set cliSet = clinit.keySet();
		List order = new LinkedList();
		int maxIter = cliSet.size();

		// maximum loop bound detects cyclic dependency
		for (int i=0; i<maxIter && cliSet.size()!=0; ++i) {

			Iterator itCliSet = cliSet.iterator();
			while (itCliSet.hasNext()) {			
				ClassInfo clinf = (ClassInfo) itCliSet.next();
				Set depends = (Set) clinit.get(clinf);
				if (depends.size()==0) {
					order.add(clinf);
					// check all depends sets and remove the added
					// element (a leave in the dependent tree
					Iterator itCliSetInner = clinit.keySet().iterator();
					while (itCliSetInner.hasNext()) {
						ClassInfo clinfInner = (ClassInfo) itCliSetInner.next();
						Set dep = (Set) clinit.get(clinfInner);
						dep.remove(clinf);
					}
					itCliSet.remove();
				}
			}
		}
		
		if (cliSet.size()!=0) {
			throw new Error("Cyclic dependency in <clinit>");
		}
		
		return order;
	}
	
	private Set findDependencies(Method method) {

		Set depends = new HashSet();

		ConstantPool cpool = clazz.getConstantPool();
		ConstantPoolGen cpoolgen = new ConstantPoolGen(cpool);
		
		MethodGen mg  = new MethodGen(method, clazz.getClassName(), cpoolgen);
		InstructionList il  = mg.getInstructionList();
		InstructionFinder f = new InstructionFinder(il);
		
		// find instructions that access the constant pool
		// collect all indices to constants in ClassInfo
		String cpInstr = "CPInstruction";		
		for(Iterator it = f.search(cpInstr); it.hasNext(); ) {
			InstructionHandle[] match = (InstructionHandle[])it.next();
			InstructionHandle   first = match[0];
			
			CPInstruction ii = (CPInstruction)first.getInstruction();
			int idx = ii.getIndex();
			Constant co = cpool.getConstant(idx);
			ConstantClass cocl = null;
			switch(co.getTag()) {
			case Constants.CONSTANT_Class:
				cocl = (ConstantClass) co;
				break;
			case Constants.CONSTANT_InterfaceMethodref:
				cocl = (ConstantClass) cpool.getConstant(((ConstantInterfaceMethodref) co).getClassIndex());
				break;
			case Constants.CONSTANT_Methodref:
				cocl = (ConstantClass) cpool.getConstant(((ConstantMethodref) co).getClassIndex());
				break;
			case Constants.CONSTANT_Fieldref:
				cocl = (ConstantClass) cpool.getConstant(((ConstantFieldref) co).getClassIndex());
				break;
			}
			if (cocl!=null) {
				String clname = cocl.getBytes(cpool).replace('/','.');
				ClassInfo clinf = (ClassInfo) ClassInfo.mapClassNames.get(clname);
				if (clinf.getMethodInfo(JOPizer.clinitSig)!=null) {
					// don't add myself as dependency
					if (clinf!=cli) {
						depends.add(clinf);
					}
				}
			}
			
			
		}
		
		il.dispose();
		
		return depends;
	}

}