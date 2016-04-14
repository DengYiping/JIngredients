package sarf.jingredients.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import soba.core.ClassInfo;
import soba.core.FieldInfo;
import soba.core.MethodInfo;
import soba.core.signature.MethodSignatureReader;
import soba.core.signature.TypeResolver;

public class Signature {

	
	public static final String METHOD_SEPARATOR = "#";

	
	public static boolean isAnonymousClass(String name) {
		return name.matches(".+\\$([0-9]+)\\Z");
	}
	

	private static boolean isRegularMethod(MethodInfo m) {
		return !m.getMethodName().contains("$") && !m.isSynthetic();
	}
	
	
	private static boolean isRegularField(FieldInfo f) {
		return !f.getFieldName().contains("$");
	}
	
	
	public static String getDaviesClassSignature(ClassInfo c) {

		String className = Signature.normalizeType(c.getClassName(),  false);
		String superClassName = Signature.normalizeType(c.getSuperClass(), false);
		ArrayList<String> interfaces = new ArrayList<>(c.getInterfaces());
		for (int i=0; i<interfaces.size(); ++i) {
			interfaces.set(i, Signature.normalizeType(interfaces.get(i), true));
		}
		Collections.sort(interfaces);
		String interfaceSig = Signature.concat(Signature.normalizeType(superClassName, true), interfaces, ";");
		
		ArrayList<String> members = new ArrayList<String>();
		for (int i=0; i<c.getMethodCount(); i++) {
			MethodInfo m = c.getMethod(i);
			if (isRegularMethod(m)) {
				String methodSig = Signature.getMethodSignature(m, false);
				members.add(methodSig);
			}
		}

		String daviesSig = Signature.concat(className + ";" + interfaceSig, members, ";");

		return daviesSig;
	}


	public static String getClassSignature(ClassInfo c, boolean removePackage) {

		String className = Signature.normalizeType(c.getClassName(),  removePackage);
		String superClassName = Signature.normalizeType(c.getSuperClass(), removePackage);
		ArrayList<String> interfaces = new ArrayList<>(c.getInterfaces());
		for (int i=0; i<interfaces.size(); ++i) {
			interfaces.set(i, Signature.normalizeType(interfaces.get(i), removePackage));
		}
		Collections.sort(interfaces);
		
		String interfaceSig = Signature.concat(superClassName, interfaces, ";");
		
		ArrayList<String> members = new ArrayList<String>();
		for (int i=0; i<c.getMethodCount(); i++) {
			MethodInfo m = c.getMethod(i);
			if (isRegularMethod(m)) {
				String methodSig = Signature.getMethodSignature(m, removePackage);
				if (m.hasMethodBody()) {
					String bodySig = Signature.getMethodBodySignature(m.getMethodNode(), removePackage);
					members.add(methodSig + "[" + bodySig + "]");
				} else {
					members.add(methodSig + "[]");
				}
			}
		}
		for (int i=0; i<c.getFieldCount(); i++) {
			FieldInfo f = c.getField(i);
			if (isRegularField(f)) {
				String fieldSig = Signature.getFieldSignature(f, removePackage);
				members.add(fieldSig);
			}
		}
		Collections.sort(members);

		String codeSig = Signature.concat(className + ";" + interfaceSig, members, ";");

		return codeSig;
	}

	
	
	public static String getFieldSignature(FieldInfo f, boolean removePackage) {
		StringBuilder buf = new StringBuilder();
		buf.append(f.getFieldName());
		buf.append(METHOD_SEPARATOR);
		buf.append(Signature.normalizeType(f.getFieldTypeName(), removePackage));
		return buf.toString();
	}
	
	
	public static String getMethodSignature(MethodInfo m, boolean removePackage) {
		StringBuilder buf = new StringBuilder();
		buf.append(m.getMethodName());
		buf.append(METHOD_SEPARATOR);
		if (m.isStatic()) buf.append("static");
		if (m.isPublic()) buf.append("public");
		if (m.isProtected()) buf.append("protected");
		if (m.isPrivate()) buf.append("private");
		if ((m.getMethodNode().access & Opcodes.ACC_SYNCHRONIZED) != 0) buf.append("synchronized");
		buf.append(METHOD_SEPARATOR);
		for (String s: m.getMethodNode().exceptions) {
			buf.append(Signature.normalizeType(s, removePackage));
			buf.append(";");
		}
		buf.append(METHOD_SEPARATOR);
		buf.append(Signature.normalizeType(m.getReturnType(), removePackage));
		for (int i=0; i<m.getParamCount(); ++i) {
			buf.append(METHOD_SEPARATOR);
			buf.append(Signature.normalizeType(m.getParamType(i), removePackage));
		}
		return buf.toString();
	}
	

	public static String getMethodBodySignature(MethodNode node, boolean removePackage) {
		ArrayList<String> signatures = new ArrayList<String>();
		InsnList instructions = node.instructions;
		if (instructions != null && instructions.size() > 0) {
			for (int i=0; i<instructions.size(); ++i) {
				AbstractInsnNode n = instructions.get(i);
				if (n.getType() == AbstractInsnNode.METHOD_INSN) {
					MethodInsnNode m = (MethodInsnNode)n;
					StringBuilder callSig = new StringBuilder();
					callSig.append(Signature.normalizeType(m.owner, removePackage));
					callSig.append("#");
					callSig.append(m.name);
					MethodSignatureReader reader = new MethodSignatureReader(m.desc);
					for (int p=0; p<reader.getParamCount(); ++p) {
						String t = Signature.normalizeType(reader.getParamType(p), removePackage);
						callSig.append("#");
						callSig.append(t);
					}
					signatures.add(callSig.toString());
				} else if (n.getType() == AbstractInsnNode.FIELD_INSN) {
					FieldInsnNode f = (FieldInsnNode)n;
					StringBuilder fieldSig = new StringBuilder();
					fieldSig.append(Signature.normalizeType(f.owner, removePackage));
					fieldSig.append("#");
					fieldSig.append(f.name);
					fieldSig.append("#");
					fieldSig.append(Signature.normalizeType(TypeResolver.getTypeName(f.desc), removePackage));
					signatures.add(fieldSig.toString());
				}
			}
			Collections.sort(signatures);
		}
		StringBuilder builder = new StringBuilder();
		for (String s: signatures) {
			builder.append(s);
			builder.append(";");
		}
		return builder.toString();
	}
	

	
	public static String normalizeType(String typeName, boolean removePackage) {
		if (typeName == null) return "";
		if (removePackage) return removeAnonymousInteger(removePackageFromType(typeName));
		else return removeAnonymousInteger(typeName);
	}
	
	static Pattern p = Pattern.compile("\\$([0-9]+)");
	
	private static String removeAnonymousInteger(String className) {
		return p.matcher(className).replaceAll("%A");
	}
	
	private static String removePackageFromType(String desc) {
		return desc.replaceAll("[a-zA-Z_$][a-zA-Z_$0-9]*/", "");
	}
	
	public static String concat(String header, ArrayList<String> signatures, String sep) {
		StringBuilder builder = new StringBuilder(65536);
		builder.append(header);
		for (int i=0; i<signatures.size(); ++i) {
			builder.append(sep);
			builder.append(signatures.get(i));
		}
		 return builder.toString();
	}

}
