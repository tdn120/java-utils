package net.thomasnardone.utils.gen;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.thomasnardone.utils.StringUtil;
import net.thomasnardone.utils.comparator.ClassNameComparator;

public abstract class ClassGenerator extends AbstractClassGenerator {
	protected static Params getParams(final Class<?> baseClass) {
		String className = new ClassSelectionDialog(baseClass).select();
		if (className == null) {
			return null;
		}
		String packageName = new PackageSelectionDialog().select();
		if (packageName == null) {
			return null;
		}
		return new Params(className, packageName);
	}

	protected final String				className;
	protected final String				packageName;
	protected final String				paramName;
	protected final String				propName;
	private final List<String>			fields;
	private final boolean				importClass;
	private final Set<Class<?>>			imports;
	private final List<List<String>>	innerClasses;
	private final Set<String>			interfaces;
	private final List<List<String>>	methods;

	private String						parentClass;

	public ClassGenerator(final Params params) throws ClassNotFoundException {
		this(params, true);
	}

	public ClassGenerator(final Params params, final boolean importClass) throws ClassNotFoundException {
		this(params.getClassName(), params.getPackageName(), importClass);
	}

	public ClassGenerator(final String fullClassName, final String packageName) throws ClassNotFoundException {
		this(fullClassName, packageName, true);
	}

	public ClassGenerator(final String fullClassName, final String packageName, final boolean importClass)
			throws ClassNotFoundException {
		super(Class.forName(fullClassName));
		this.packageName = packageName;
		this.importClass = importClass;
		className = clazz.getSimpleName();
		paramName = StringUtil.deCapitalize(className);
		propName = StringUtil.underscore(className);
		fields = new LinkedList<String>();
		imports = new TreeSet<Class<?>>(new ClassNameComparator());
		interfaces = new TreeSet<String>(StringUtil.comparator());
		methods = new LinkedList<List<String>>();
		innerClasses = new LinkedList<List<String>>();
	}

	public final void generate() throws Exception {
		if (importClass) {
			addImport(clazz);
		}
		generateStuff();
		writeClassToFile();
	}

	protected final void addField(final String field) {
		fields.add(field);
	}

	protected final void addImport(final Class<?> importClass) {
		final String importPackage = importClass.getPackage().getName();
		if (!packageName.equals(importPackage) && !"java.lang".equals(importPackage)) {
			imports.add(importClass);
		}
	}

	protected final void addInnerClass(final List<String> innerClass) {
		innerClasses.add(innerClass);
	}

	protected final void addMethod(final List<String> method) {
		methods.add(method);
	}

	protected void extend(final Class<?> clazz, final Class<?>... params) {
		if (parentClass != null) {
			throw new IllegalStateException("Already extending a class: " + parentClass);
		}
		if ((params == null) || (params.length < 1)) {
			parentClass = clazz.getSimpleName();
		} else {
			parentClass = clazz.getSimpleName() + getParamString(params);
		}
		addImport(clazz);
	}

	protected abstract void generateStuff() throws Exception;

	protected String getConstructorName() {
		return className + getName();
	}

	protected abstract String getName();

	protected String getPackageName() {
		return packageName;
	}

	protected String getSourcePath() {
		return "src/";
	}

	protected final String getter(final Field field) {
		return (isBoolean(field.getType()) ? "is" : "get") + StringUtil.capitalize(field.getName());
	}

	protected void implement(final Class<?> clazz, final Class<?>... params) {
		if ((params == null) || (params.length < 1)) {
			interfaces.add(clazz.getSimpleName());
		} else {
			interfaces.add(clazz.getSimpleName() + getParamString(params));
		}
		addImport(clazz);
	}

	protected final boolean isBoolean(final Class<?> type) {
		return type.isPrimitive() && "boolean".equals(type.getName());
	}

	protected final String setter(final Field field) {
		return "set" + StringUtil.capitalize(field.getName());
	}

	private String getParamString(final Class<?>... params) {
		StringBuilder sb = new StringBuilder("<");
		for (Class<?> paramClass : params) {
			sb.append(paramClass.getSimpleName());
			sb.append(",");
			addImport(paramClass);
		}
		final String paramString = sb.substring(0, sb.length() - 1) + ">";
		return paramString;
	}

	private void writeClassToFile() throws FileNotFoundException {
		openWriter(new File(getSourcePath() + packageName.replaceAll("\\.", "/") + "/" + className + getName() + ".java"));
		writeln("package " + packageName + ";");
		writeln();
		for (Class<?> importClass : imports) {
			writeln("import " + importClass.getName().replaceAll("\\$", ".") + ";");
		}

		writeln();
		writeln("/**");
		writeln(" * Auto-generated by {@link " + getClass().getName() + "}.");
		writeln(" */");
		write("public class " + className + getName());
		if (parentClass != null) {
			write(" extends " + parentClass);
		}
		if (!interfaces.isEmpty()) {
			write(" implements ");
			final Iterator<String> iterator = interfaces.iterator();
			write(iterator.next());
			while (iterator.hasNext()) {
				write(", " + iterator.next());
			}
		}
		writeln(" {");

		if (!fields.isEmpty()) {
			for (String field : fields) {
				writeln(field + ";");
			}
			writeln();
		}

		if (!methods.isEmpty()) {
			for (List<String> lines : methods) {
				for (String line : lines) {
					writeln(line);
				}
				writeln();
			}
		}

		if (!innerClasses.isEmpty()) {
			for (List<String> lines : innerClasses) {
				for (String line : lines) {
					writeln(line);
				}
				writeln();
			}
		}
		writeln("}");

		closeWriter();
	}

	public static class Params {
		private final String	className;
		private final String	packageName;

		public Params(final String className, final String packageName) {
			this.packageName = packageName;
			this.className = className;
		}

		public String getClassName() {
			return className;
		}

		public String getPackageName() {
			return packageName;
		}
	}
}
