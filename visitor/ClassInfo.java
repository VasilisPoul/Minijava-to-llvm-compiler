package visitor;

import java.util.LinkedHashMap;

public class ClassInfo {

	public String name;
	public String parent;
	public LinkedHashMap<String, MethodClass> methods;

	public ClassInfo(String name, String parent) {
		this.name = name;
		this.parent = parent;
		this.methods = new LinkedHashMap<String, MethodClass>();
	}
}
