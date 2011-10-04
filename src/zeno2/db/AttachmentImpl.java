package zeno2.db;

import java.io.File;
import java.io.InputStream;

import java.sql.ResultSet;
import java.util.Hashtable;
import java.util.List;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import zeno2.kernel.Attachment;
import zeno2.kernel.ZenoException;

public class AttachmentImpl {

    final private static String ZENO_ATTACHMENT_ATTACHMENTMODE_PROPERTYNAME = "zenoAttachmentMode";
    final private static String ZENO_ATTACHMENT_ATTACHMENTMODE_DEFAULT  = "AttachmentInDBImpl";
    final private static String ZENO_ATTACHMENT_METHOD_COPY_ATTACHEMENT = "copyAttachments";
    final private static String ZENO_ATTACHMENT_METHOD_DELETE_ATTACHEMENT = "deleteAttachment";
    final private static String ZENO_ATTACHMENT_METHOD_DELETE_ATTACHEMENTS = "deleteAttachments";
    final private static String ZENO_ATTACHMENT_CLASS_HOME = "zeno2.db.";

    private AttachmentImpl() {}

    public static Attachment createAttachment(FactoryImpl factory,
					      int         articleId,
					      String      name,
					      String      mimeType,
					      File        file) throws ZenoException {
		
		Class  []    parameterTypes = new Class[5];
		Object []    initargs = new Object[5];

		Integer      iArticleId = new Integer(articleId);

		parameterTypes[0] = factory.getClass();
		parameterTypes[1] = iArticleId.getClass();
		parameterTypes[2] = name.getClass();
		parameterTypes[3] = mimeType.getClass();
		parameterTypes[4] = file.getClass();

		initargs[0] = factory;
		initargs[1] = iArticleId;
		initargs[2] = name;
		initargs[3] = mimeType;
		initargs[4] = file;

		return createInstance(factory, parameterTypes, initargs);

    }


    public static Attachment createAttachment(FactoryImpl factory,
					      int         articleId,
					      String      name,
					      String      mimeType,
					      InputStream inputStream) throws ZenoException {
		
		Class  []    parameterTypes = new Class[5];
		Object []    initargs = new Object[5];

		Integer      iArticleId = new Integer(articleId);

		parameterTypes[0] = factory.getClass();
		parameterTypes[1] = iArticleId.getClass();
		parameterTypes[2] = name.getClass();
		parameterTypes[3] = mimeType.getClass();
		parameterTypes[4] = InputStream.class;

		initargs[0] = factory;
		initargs[1] = iArticleId;
		initargs[2] = name;
		initargs[3] = mimeType;
		initargs[4] = inputStream;

		return createInstance(factory, parameterTypes, initargs);

    }


    public static Attachment getAttachment(FactoryImpl factory,
					   ResultSet   rs) throws ZenoException {
			

		Class  []    parameterTypes = new Class[2];
		Object []    initargs = new Object[2];
		
		parameterTypes[0] = factory.getClass();
		parameterTypes[1] = rs.getClass().getInterfaces()[0];

		initargs[0] = factory;
		initargs[1] = rs;

		return createInstance(factory, parameterTypes, initargs);
    }
	

    public static void copyAttachments(FactoryImpl factory,
				       List attachments,
				       Hashtable newids) throws ZenoException {

		Class  [] parameterTypes = new Class[3];
		Object [] initargs = new Object[3];


		parameterTypes[0] = factory.getClass();
		parameterTypes[1] = attachments.getClass().getInterfaces()[0];
		parameterTypes[2] = newids.getClass();

		initargs[0] = factory;
		initargs[1] = attachments;
		initargs[2] = newids;

		invokeMethod(factory, ZENO_ATTACHMENT_METHOD_COPY_ATTACHEMENT, parameterTypes, initargs);
    }

	
    public static void deleteAttachment(FactoryImpl factory,
					int         articleId,
					int         attachmentId) throws ZenoException {

		Class  [] parameterTypes = new Class[3];
		Object [] initargs = new Object[3];
		
		Integer iArticleId = new Integer(articleId);
		Integer iAttachmentId = new Integer(attachmentId);

		Class       attachmentClass;
		Method      attachmentMethod;

		parameterTypes[0] = factory.getClass();
		parameterTypes[1] = iArticleId.getClass();
		parameterTypes[2] = iAttachmentId.getClass();

		initargs[0] = factory;
		initargs[1] = iArticleId;
		initargs[2] = iAttachmentId;

		invokeMethod(factory, ZENO_ATTACHMENT_METHOD_DELETE_ATTACHEMENT, parameterTypes, initargs);
    }


    public static void deleteAttachments(FactoryImpl factory,
					 int         articleId) throws ZenoException {

		Class  [] parameterTypes = new Class[2];
		Object [] initargs = new Object[2];

		Integer iArticleId = new Integer(articleId);

		parameterTypes[0] = factory.getClass();
		parameterTypes[1] = iArticleId.getClass();

		initargs[0] = factory;
		initargs[1] = iArticleId;

		invokeMethod(factory, ZENO_ATTACHMENT_METHOD_DELETE_ATTACHEMENTS, parameterTypes, initargs);
    }


    private static Attachment createInstance(FactoryImpl factory,
					     Class  []   parameterTypes,
					     Object []   initargs) throws ZenoException {

		String      attachmentClassName;
		Class       attachmentClass;
		Constructor attachmentConstructor;
		Logger      log = Logger.getLogger(AttachmentImpl.class.getName());


		attachmentClassName = ZENO_ATTACHMENT_CLASS_HOME + 
		                         factory.getMonitor().getProperty(ZENO_ATTACHMENT_ATTACHMENTMODE_PROPERTYNAME, 
									  ZENO_ATTACHMENT_ATTACHMENTMODE_DEFAULT);

		try {
		    attachmentClass = Class.forName(attachmentClassName);
		    attachmentConstructor = attachmentClass.getDeclaredConstructor(parameterTypes);
		    
		    return ((Attachment) attachmentConstructor.newInstance(initargs));

		} catch (java.lang.ClassNotFoundException e) {
		    String errorText = "can't find specified class " + attachmentClassName + " (ClassNotFoundException)";

		    factory.reportError("AttachmentImpl.getAttachmentClass ", e);
		    log.error(errorText);
		    throw new ZenoException(errorText);

		} catch (java.lang.NoSuchMethodException e) {
		    String errorText = "class " + attachmentClassName + " does not provide appropriate constructor";

		    factory.reportError("AttachmentImpl.creator", e) ;
		    log.error(errorText);
		    throw new ZenoException(errorText);

		} catch (java.lang.InstantiationException e) {
		    String errorText = "can't instatiate object of class " + attachmentClassName;

		    factory.reportError("AttachmentImpl.creator", e) ;
		    log.error(errorText);
		    throw new ZenoException(errorText);

		} catch (java.lang.IllegalAccessException e) {
		    String errorText = "can't invoke Method in " + attachmentClassName + " (IllegalAccessException)";

		    factory.reportError("AttachmentImpl.creator", e) ;
		    log.error(errorText);
		    throw new ZenoException(errorText);

		} catch (java.lang.reflect.InvocationTargetException e) {
		    String errorText = "error while invoking method in " + attachmentClassName + " (InvocationTargetException)";

		    factory.reportError("AttachmentImpl.creator", e) ;
		    log.error(errorText);
		    throw new ZenoException(errorText);

		}

    }


    private static void invokeMethod(FactoryImpl factory,
				     String      methodName,
				     Class []    parameterTypes,
				     Object[]    initargs) throws ZenoException {

		Class  attachmentClass;
		Method attachmentMethod;
		Logger log = Logger.getLogger(AttachmentImpl.class.getName());

		String attachmentClassName = ZENO_ATTACHMENT_CLASS_HOME + 
		                                            factory.getMonitor().getProperty(ZENO_ATTACHMENT_ATTACHMENTMODE_PROPERTYNAME, 
											     ZENO_ATTACHMENT_ATTACHMENTMODE_DEFAULT);	


		try {
		    attachmentClass = Class.forName(attachmentClassName);
		    attachmentMethod = attachmentClass.getDeclaredMethod(methodName, parameterTypes);

		    attachmentMethod.invoke(attachmentClass, initargs);

		} catch (java.lang.ClassNotFoundException e) {
		    String errorText = "can't find specified class " + attachmentClassName + " (ClassNotFoundException)";

		    factory.reportError("AttachmentImpl.getAttachmentClass", e);
		    log.error(errorText);
		    throw new ZenoException(errorText);

		} catch (java.lang.NoSuchMethodException e) {
		    String errorText = "class " + attachmentClassName + " does not provide specified method (NoSuchMethodException)";

	            factory.reportError("AttachmentImpl.creator", e);
		    log.error(errorText);
	            throw new ZenoException(errorText);

		} catch (java.lang.IllegalAccessException e) {
		    String errorText = "can't invoke Method in " + attachmentClassName + " (IllegalAccessException)";

	            factory.reportError("AttachmentImpl.creator", e);
		    log.error(errorText);
	            throw new ZenoException(errorText);

		} catch (java.lang.reflect.InvocationTargetException e) {
		    String errorText = "error while invoking method in " + attachmentClassName + " (InvocationTargetException)";

	            factory.reportError("AttachmentImpl.creator", e);
		    log.error(errorText);
	            throw new ZenoException(errorText);

		}

    }

}
