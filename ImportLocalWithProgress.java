package com.incquerylabs.arrowhead.magicdraw.vorto;

import com.incquerylabs.arrowhead.magicdraw.vorto.auto.VortoProfile;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.sysml.util.SysMLProfile;
import com.nomagic.task.ProgressStatus;
import com.nomagic.task.RunnableWithProgress;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mddependencies.Dependency;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Enumeration;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Operation;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Parameter;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Type;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.impl.ElementsFactory;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.eclipse.vorto.core.api.model.model.Model;
import org.eclipse.vorto.model.*;
import org.eclipse.vorto.utilities.reader.IModelWorkspace;
import org.eclipse.vorto.utilities.reader.ModelWorkspaceReader;

class ImportLocalWithProgress implements RunnableWithProgress {

    private File file;
    Logger logger;

    private Map<ModelId, AbstractModel> models;

    private Package importsPackage;
    private Package dictionariesPackage;
    private final ModelElementsManager manager = ModelElementsManager.getInstance();
    private ElementsFactory factory;
    private VortoProfile profile;

    private final Map<ModelId, Type> elements = new HashMap<>();
    private final Map<String, Class> dictionaries = new HashMap<>();
    private final Map<String, Package> namespace = new HashMap<>();

    private static final String IMPORTS_PACKAGE_NAME = "Imported Elements";

    public ImportLocalWithProgress(File file) {
        this.file = file;
        logger = Logger.getLogger(this.getClass());
    }

    @Override
    public void run(ProgressStatus status) {
        //status.setMax(ids.size());
        Project project = Application.getInstance().getProject();
        if(project != null) {
            profile = VortoProfile.getInstance(project);
            SessionManager.getInstance().createSession(project, "Import from local Vorto model");
            factory = project.getElementsFactory();
            Package rootElement = project.getPrimaryModel();
            //FileInputStream fileInputStream = null;

            ModelWorkspaceReader.init();
            ModelWorkspaceReader workspace = IModelWorkspace.newReader();


                try {
                    getFilesFromZip().entrySet()
                            .forEach(entry -> workspace.addFile(entry.getKey(), entry.getValue()));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                IModelWorkspace ws = workspace.read();
                for (Model model : ws.get()) {
                                java.lang.Class<? extends Model> unloader = model.getClass();
                                if(Infomodel.class.isAssignableFrom(unloader)) {
                                    importInfoModel((Infomodel) model);
                                } else if(FunctionblockModel.class.isAssignableFrom(unloader)) {
                                    importFunctionBlock((FunctionblockModel) model);
                                } else if(EntityModel.class.isAssignableFrom(unloader)) {
                                    importEntity((EntityModel) model);
                                }
                            }
                status.increase();


            if(!status.isCancel()) {
                setOwnerOfNewElement(importsPackage, rootElement);
            } else {
                try {
                    manager.removeElement(importsPackage);
                } catch(ReadOnlyElementException e) {
                    e.printStackTrace();
                    //the package only contains elements added in this session, therefore cannot be read only
                }
            }
            SessionManager.getInstance().closeSession(project);
        }
    }


    protected Map<InputStream,ModelType> getFilesFromZip() throws FileNotFoundException {
        //Collection<InputStream> fileUploads = new ArrayList<>();
        Map<InputStream,ModelType> map = new HashMap<>();
        //ZipInputStream zis = new ZipInputStream(new ZipInputStream(zipFile.getInputStream()));

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
        ZipEntry ze;
        ModelType mt = null;

        try {
            ze = zis.getNextEntry();

            while (ze != null) {

                InputStream stream = zipFile.getInputStream(ze);

                switch (ze.getName().substring(ze.getName().lastIndexOf(".") + 1)) {
                    case "infomodel":
                        mt = ModelType.InformationModel;
                        break;
                    case "fbmodel":
                        mt = ModelType.Functionblock;
                        break;
                    case "type":
                        mt = ModelType.Datatype;
                        break;
                }

                map.put(stream, mt);

                ze = zis.getNextEntry();
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return map;
    }

    private Type importModel(AbstractModel model) {
        Type element;
        java.lang.Class<? extends AbstractModel> unloader = model.getClass();
        if(Infomodel.class.isAssignableFrom(unloader)) {
            element = importInfoModel((Infomodel) model);
        } else if(FunctionblockModel.class.isAssignableFrom(unloader)) {
            element = importFunctionBlock((FunctionblockModel) model);
        } else if(EntityModel.class.isAssignableFrom(unloader)) {
            element = importEntity((EntityModel) model);
        } else if(EnumModel.class.isAssignableFrom(unloader)) {
            element = importEnumeration((EnumModel) model);
        } else {
            logger.error("importModel for " + model.getClass().getName() + " not implemented yet");
            element = null;
        }
        return element;
    }

    private Type importInfoModel(Infomodel model) {
        return elements.computeIfAbsent(model.getId(), id -> {
            Class element = factory.createClassInstance();
            setOwnerOfNewElement(element, importPackage(id.getNamespace()));
            StereotypesHelper.addStereotype(element, profile.getInformationModel());
            importAbstractModel(model, element);

            for(ModelProperty property : model.getFunctionblocks()) {
                Type functionBlock = importType(property.getType());
                importInformationModelContains(property, element, functionBlock);
            }
            return element;
        });
    }

    private Type importFunctionBlock(FunctionblockModel model) {
        return elements.computeIfAbsent(model.getId(), id -> {
            Class element = factory.createClassInstance();
            setOwnerOfNewElement(element, importPackage(id.getNamespace()));
            StereotypesHelper.addStereotype(element, profile.getFunctionBlock());
            importAbstractModel(model, element);

            for(ModelProperty property : model.getConfigurationProperties()) {
                importProperty(property, element, profile.getVortoConfiguration());
            }
            for(ModelProperty property : model.getStatusProperties()) {
                importProperty(property, element, profile.getVortoStatus());
            }
            for(ModelEvent event : model.getEvents()) {
                importEvent(event, element);
            }
            for(org.eclipse.vorto.model.Operation operation : model.getOperations()) {
                importOperation(operation, element);
            }
            //TODO faults
            return element;
        });
    }

    private Type importEnumeration(EnumModel model) {
        return elements.computeIfAbsent(model.getId(), id -> {
            Enumeration element = factory.createEnumerationInstance();
            setOwnerOfNewElement(element, importPackage(id.getNamespace()));
            StereotypesHelper.addStereotype(element, profile.getVortoEnumeration());
            importAbstractModel(model, element);

            for(EnumLiteral literal : model.getLiterals()) {
                EnumerationLiteral literalElement = factory.createEnumerationLiteralInstance();
                element.getOwnedLiteral().add(literalElement);
                literalElement.setName(literal.getName());
                VortoProfile.VortoLiteral.setDescription(literalElement, literal.getDescription());
            }
            return element;
        });
    }

    private Type importEntity(EntityModel model) {
        return elements.computeIfAbsent(model.getId(), id -> {
            Class element = factory.createClassInstance();
            setOwnerOfNewElement(element, importPackage(id.getNamespace()));
            StereotypesHelper.addStereotype(element, profile.getVortoEntity());
            importAbstractModel(model, element);

            for(ModelProperty property : model.getProperties()) {
                importProperty(property, element, profile.getVortoProperty());
            }
            return element;
        });
    }

    private Type importType(IReferenceType type) {
        java.lang.Class<? extends IReferenceType> unloader = type.getClass();
        Type element;
        if(ModelId.class.isAssignableFrom(unloader)) {
            element = importById((ModelId) type);
        } else if(DictionaryType.class.isAssignableFrom(unloader)) {
            element = importDictionary((DictionaryType) type);
        } else if(PrimitiveType.class.isAssignableFrom(unloader)) {
            element = getPrimitiveType((PrimitiveType) type);
        } else {
            logger.error("importType for " + type.getClass().getName() + " not implemented yet");
            element = null;
        }
        return element;
    }

    private Type importDictionary(DictionaryType type) {
        return dictionaries.computeIfAbsent(type.toString(), id -> {
            Class element = factory.createClassInstance();
            StereotypesHelper.addStereotype(element, profile.getDictionary());
            setOwnerOfNewElement(element, dictionariesPackage);
            Type key = importType(type.getKey());
            Type value = importType(type.getValue());
            if(key != null && value != null) {
                element.setName(key.getName() + "-To-" + value.getName() + "-Dictionary");
            } else {
                element.setName("Wrong Dictionary");
            }
            VortoProfile.Dictionary.setKey(element, key);
            VortoProfile.Dictionary.setValue(element, value);
            return element;
        });
    }

    private Type importById(ModelId type) {
        AbstractModel model = models.get(type);
        return importModel(model);
    }

    private Type getPrimitiveType(PrimitiveType type) {
        Type element = profile.getInteger();
        SysMLProfile sysML = SysMLProfile.getInstance(element);
        switch(type) {
            case STRING:
                element = sysML.getString();
                break;
            case INT:
                element = profile.getInteger();
                break;
            case FLOAT:
                element = profile.getFloat();
                break;
            case BOOLEAN:
                element = sysML.getBoolean();
                break;
            case DATETIME:
                element = profile.getDateTime();
                break;
            case DOUBLE:
                element = profile.getDouble();
                break;
            case LONG:
                element = profile.getLong();
                break;
            case SHORT:
                element = profile.getShort();
                break;
            case BASE64_BINARY:
                element = profile.getBase64();
                break;
            case BYTE:
                element = profile.getByte();
                break;
        }
        return element;
    }

    private Package importPackage(String name) {
        if("".equals(name)) {
            return importsPackage;
        } else {
            return namespace.computeIfAbsent(name, string -> {
                int index = string.lastIndexOf(".");
                String parentNamespace = string.substring(0, index == -1 ? 0 : index);
                Package parentPackage = importPackage(parentNamespace);
                Package thisPackage = factory.createPackageInstance();
                thisPackage.setName(string.substring(index + 1));
                setOwnerOfNewElement(thisPackage, parentPackage);
                return thisPackage;
            });
        }
    }

    private void importProperty(ModelProperty property, Class parent, Stereotype stereotype) {
        Property element = factory.createPropertyInstance();
        StereotypesHelper.addStereotype(element, stereotype);
        int lower = property.isMandatory() ? 1 : 0;
        int upper = property.isMultiple() ? -1 : 1;
        ModelHelper.setMultiplicity(lower, upper, element);
        Type type = importType(property.getType());
        element.setType(type);
        element.setName(property.getName());
        parent.getFeature().add(element);
        VortoProfile.VortoProperty.setDescription(element, property.getDescription());
    }

    private void importEvent(ModelEvent event, Class functionBlock) {
        Class element = factory.createClassInstance();
        setOwnerOfNewElement(element, functionBlock);
        StereotypesHelper.addStereotype(element, profile.getEvent());
        element.setName(event.getName());
        for(ModelProperty property : event.getProperties()) {
            importProperty(property, element, profile.getVortoProperty());
        }
    }

    private void importOperation(org.eclipse.vorto.model.Operation operation, Class functionBlock) {
        Operation element = factory.createOperationInstance();
        StereotypesHelper.addStereotype(element, profile.getVortoOperation());
        element.setName(operation.getName());
        VortoProfile.VortoOperation.setBreakable(element, operation.isBreakable());
        VortoProfile.VortoOperation.setDescription(element, operation.getDescription());
        for(Param param : operation.getParams()) {
            Parameter parameterElement = factory.createParameterInstance();
            int lower = param.isMandatory() ? 1 : 0;
            int upper = param.isMultiple() ? -1 : 1;
            ModelHelper.setMultiplicity(lower, upper, parameterElement);
            Type type = importType(param.getType());
            parameterElement.setType(type);
            parameterElement.setName(param.getName());
            element.getOwnedParameter().add(parameterElement);
        }
        ReturnType result = operation.getResult();
        if(result != null) {
            Parameter resultElement = factory.createParameterInstance();
            ModelHelper.setMultiplicity(1, result.isMultiple() ? -1 : 1, resultElement);
            Type type = importType(result.getType());
            resultElement.setType(type);
            resultElement.setName("return");
            element.getOwnedParameter().add(resultElement);
        }
        functionBlock.getFeature().add(element);
    }

    private void importAbstractModel(AbstractModel model, NamedElement element) {
        ModelId id = model.getId();
        element.setName(model.getDisplayName());
        VortoProfile.Model.setCategory(element, model.getCategory());
        VortoProfile.Model.setIdentifier(element, id.getName());
        VortoProfile.Model.setNamespace(element, id.getNamespace());
        VortoProfile.Model.setVersion(element, id.getVersion());
        VortoProfile.Model.setVortolang(element, VortoProfile.VortoVersionEnum._1_0);
        VortoProfile.Model.setDescription(element, model.getDescription());
    }

    private void importInformationModelContains(ModelProperty model, Class infomodel, Type functionBlock) {
        Dependency element = factory.createDependencyInstance();
        element.getClient().add(infomodel);
        element.getSupplier().add(functionBlock);
        if(infomodel.getPackage() != null) {
            setOwnerOfNewElement(element, infomodel.getPackage());
        } else {
            setOwnerOfNewElement(element, importsPackage);
        }
        StereotypesHelper.addStereotype(element, profile.getContains());
        VortoProfile.Contains.setIdentifier(element, model.getName());
        VortoProfile.Contains.setMandatory(element, model.isMandatory());
        VortoProfile.Contains.setMultiple(element, model.isMultiple());
        VortoProfile.Contains.setDescription(element, model.getDescription());
    }

    private void setOwnerOfNewElement(Element element, Element owner) {
        try {
            manager.addElement(element, owner);
        } catch(ReadOnlyElementException e) {
            //new elements cannot be read-only
            e.printStackTrace();
        }
    }
}

