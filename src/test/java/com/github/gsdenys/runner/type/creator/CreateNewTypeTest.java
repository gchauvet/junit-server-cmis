package com.github.gsdenys.runner.type.creator;

import com.github.gsdenys.CmisInMemoryRunner;
import com.github.gsdenys.runner.utils.CmisUtils;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gsdenys on 11/02/17.
 */
@RunWith(CmisInMemoryRunner.class)
public class CreateNewTypeTest {
    private TypeCreator createNewType;

    @Before
    public void setUp() throws Exception {
        if (this.createNewType == null) {
            this.createNewType = new TypeCreator();
        }
    }

    @Test
    public void execute() throws Exception {
        DocumentTypeDefinitionImpl docDef = new DocumentTypeDefinitionImpl();

        docDef.setId("tst:doctype");
        docDef.setBaseTypeId(BaseTypeId.CMIS_DOCUMENT);
        docDef.setDescription("a new type");
        docDef.setDisplayName("Test Document Type");
        docDef.setLocalName("DocType");
        docDef.setParentTypeId("cmis:document");
        docDef.setLocalNamespace("tst");

        Map<String, PropertyDefinition<?>> propertyDefinitions = new HashMap<>();

        PropertyBooleanDefinitionImpl booleanDefinition = new PropertyBooleanDefinitionImpl();
        booleanDefinition.setId("tst:boolean");
        booleanDefinition.setLocalName("boolean");
        booleanDefinition.setDisplayName("Test Boolean");
        booleanDefinition.setDescription("A boolean test");
        booleanDefinition.setLocalNamespace("tst");
        booleanDefinition.setPropertyType(PropertyType.BOOLEAN);
        booleanDefinition.setQueryName("tst:boolean");

        propertyDefinitions.put(booleanDefinition.getId(), booleanDefinition);
        docDef.setPropertyDefinitions(propertyDefinitions);


        this.createNewType.execute(docDef);

        //assert
        CmisUtils util = new CmisUtils();
        Session session = util.getSession(null);

        ObjectType objType = session.getTypeDefinition("tst:doctype");

        Assert.assertNotNull("The Object type should not be null", objType);
        Assert.assertEquals(
                "The object type local namespace should be 'tst'",
                objType.getLocalNamespace(),
                "tst"
        );
    }
}