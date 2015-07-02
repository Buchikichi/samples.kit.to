package to.kit.samples;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import ognl.Ognl;
import ognl.OgnlException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import to.kit.samples.dto.NoAccessorDto;

/**
 * javassist sample.
 * @author H.Sasai
 */
public class SampleApp {
	private void testOfGetProperty(NoAccessorDto dto) {
		try {
			Object name = PropertyUtils.getProperty(dto, "name");
			System.out.println("name: " + name);
		} catch (IllegalAccessException | InvocationTargetException
				| NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	private void testOfDescribe(NoAccessorDto dto) {
		try {
			NoAccessorDto obj = (NoAccessorDto) BeanUtils.cloneBean(dto);
			System.out.println(BeanUtils.describe(obj));
		} catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	private String makeGetterString(CtField field) throws NotFoundException {
		StringBuilder buff = new StringBuilder();
		String fieldName = field.getName();
		String type = field.getType().getName();
		buff.append("public ");
		buff.append(type);
		buff.append(" get");
		buff.append(StringUtils.capitalize(fieldName));
		buff.append("(){return ");
		buff.append(fieldName);
		buff.append(";};");
		return buff.toString();
	}

	private String makeSetterString(CtField field) throws NotFoundException {
		StringBuilder buff = new StringBuilder();
		String fieldName = field.getName();
		String type = field.getType().getName();
		buff.append("public void set");
		buff.append(StringUtils.capitalize(fieldName));
		buff.append("(");
		buff.append(type);
		buff.append(" a){");
		buff.append(fieldName);
		buff.append("=a;};");
		return buff.toString();
	}

	private void makeMethods(CtClass ctClass) throws NotFoundException, CannotCompileException {
		for (CtField field : ctClass.getFields()) {
			String getterString = makeGetterString(field);
			String setterString = makeSetterString(field);
			CtMethod getterMethod = CtNewMethod.make(getterString, ctClass);
			CtMethod setterMethod = CtNewMethod.make(setterString, ctClass);

			ctClass.addMethod(getterMethod);
			ctClass.addMethod(setterMethod);
		}
	}

	private <T> void copyFields(T dest, T orig) {
		Class<?> clazz = dest.getClass();
		for (Field field : FieldUtils.getAllFields(clazz)) {
			String fieldName = field.getName();
			try {
				Object value = Ognl.getValue(fieldName, orig);
				Ognl.setValue(fieldName, dest, value);
			} catch (OgnlException e) {
				// nop
			}
		}
	}

	private <T> T addMethod(T orig) {
		T result = null;
		Class<?> srcClass = orig.getClass();
		String className = srcClass.getName();
		ClassPool cp = ClassPool.getDefault();
		try {
			CtClass superClass = cp.get(className);
			CtClass ctClass = cp.makeClass(className + "0");

			ctClass.setSuperclass(superClass);
			makeMethods(ctClass);
			Class<T> clazz = ctClass.toClass();
			result = clazz.newInstance();
			copyFields(result, orig);
		} catch (NotFoundException | CannotCompileException
				| InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return result;
	}

	private void execute() {
		NoAccessorDto dto = new NoAccessorDto();

		dto.code = "Code";
		dto.name = "Name";
		dto.value = 331;
		testOfGetProperty(dto);
		testOfDescribe(dto);

		// after
		NoAccessorDto newDto = addMethod(dto);

		testOfGetProperty(newDto);
		testOfDescribe(newDto);
	}

	public static void main(String[] args) {
		SampleApp app = new SampleApp();

		app.execute();
	}
}
