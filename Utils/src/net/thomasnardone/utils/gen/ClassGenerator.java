package net.thomasnardone.utils.gen;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.thomasnardone.utils.StringUtil;
import net.thomasnardone.utils.comparator.ClassNameComparator;

public abstract class ClassGenerator extends AbstractGenerator {
	protected static Params getParams(final Class<?> baseClass) {
		String className = new ClassSelectionDialog(baseClass).select();
		String packageName = new PackageSelectionDialog().select();
		return new Params(className, packageName);
	}

	protected final String				className;
	protected final Class<?>			clazz;
	protected final String				packageName;
	protected final String				paramName;
	protected final String				propName;
	private final List<Field>			declaredFields;
	private final List<String>			fields;
	private final boolean				importClass;
	private final Set<Class<?>>			imports;
	private final List<List<String>>	innerClasses;
	private final Set<Class<?>>			interfaces;
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
		this.packageName = packageName;
		this.importClass = importClass;
		clazz = Class.forName(fullClassName);
		className = clazz.getSimpleName();
		paramName = StringUtil.deCapitalize(className);
		propName = StringUtil.underscore(className);
		fields = new LinkedList<String>();
		imports = new TreeSet<Class<?>>(new ClassNameComparator());
		interfaces = new TreeSet<Class<?>>(new ClassNameComparator());
		methods = new LinkedList<List<String>>();
		innerClasses = new LinkedList<List<String>>();
		declaredFields = new LinkedList<Field>();
		for (Field field : clazz.getDeclaredFields()) {
			if (!(Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))) {
				declaredFields.add(field);
			}
		}
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
		imports.add(importClass);
	}

	protected final void addInnerClass(final List<String> innerClass) {
		innerClasses.add(innerClass);
	}

	protected final void addMethod(final List<String> method) {
		methods.add(method);
	}

	protected void extend(final Class<?> clazz) {
		if (parentClass != null) {
			throw new IllegalStateException("Already extending a class: " + parentClass);
		}
		parentClass = clazz.getSimpleName();
		addImport(clazz);
	}

	protected void extend(final String className) {
		if (parentClass != null) {
			throw new IllegalStateException("Already extending a class: " + parentClass);
		}
		parentClass = className;
	}

	protected abstract void generateStuff() throws Exception;

	protected String getConstructorName() {
		return className + getName();
	}

	protected List<Field> getDeclaredFields() {
		return declaredFields;
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

	protected void implement(final Class<?> clazz) {
		interfaces.add(clazz);
		addImport(clazz);
	}

	protected final boolean isBoolean(final Class<?> type) {
		return type.isPrimitive() && "boolean".equals(type.getName());
	}

	protected final String setter(final Field field) {
		return "set" + StringUtil.capitalize(field.getName());
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
			final Iterator<Class<?>> iterator = interfaces.iterator();
			write(iterator.next().getSimpleName());
			while (iterator.hasNext()) {
				write(", " + iterator.next().getSimpleName());
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
