/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 1.1.5
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.model.integration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchOrders;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.model.integration.VerifyHooks.HookType;

public class CrudIntegrationTest extends IntegrationTest {

    @Test
    public void shouldSaveEntity() throws Exception {
        // given
        DataDefinition productDataDefinition = dataDefinitionService.get(PLUGIN_PRODUCTS_NAME, ENTITY_NAME_PRODUCT);

        Entity product = createProduct("asd", "def");

        // when
        product = productDataDefinition.save(product);

        // then
        assertEquals("asd", product.getField("name"));
        assertEquals("def", product.getField("number"));
        assertNotNull(product.getId());
        assertTrue(product.isValid());
        assertEquals("product", product.getDataDefinition().getName());
        assertEquals("products", product.getDataDefinition().getPluginIdentifier());

        Map<String, Object> result = jdbcTemplate.queryForMap("select * from " + TABLE_NAME_PRODUCT);

        assertNotNull(result);
        assertEquals(product.getId(), result.get("id"));
        assertEquals("asd", result.get("name"));
        assertEquals("def", result.get("number"));

        assertEquals(1, verifyHooks.getNumOfInvocations(HookType.SAVE));
        assertEquals(1, verifyHooks.getNumOfInvocations(HookType.CREATE));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.COPY));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.UPDATE));
    }

    @Test
    public void shouldCopyEntity() throws Exception {
        // given
        DataDefinition productDataDefinition = dataDefinitionService.get(PLUGIN_PRODUCTS_NAME, ENTITY_NAME_PRODUCT);

        Entity product = productDataDefinition.save(createProduct("asd", "def"));

        verifyHooks.clear();

        // when
        Entity productCopy = productDataDefinition.copy(product.getId()).get(0);

        // then
        assertEquals(product.getField("name"), productCopy.getField("name"));
        assertEquals(product.getField("number") + "(1)", productCopy.getField("number"));
        assertNotNull(productCopy.getId());
        assertFalse(productCopy.getId().equals(product.getId()));
        assertTrue(productCopy.isValid());
        assertEquals(product.getDataDefinition().getName(), productCopy.getDataDefinition().getName());
        assertEquals(product.getDataDefinition().getPluginIdentifier(), productCopy.getDataDefinition().getPluginIdentifier());

        List<Map<String, Object>> result = jdbcTemplate.queryForList("select * from " + TABLE_NAME_PRODUCT + " order by id asc");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(product.getId(), result.get(0).get("id"));
        assertEquals(productCopy.getId(), result.get(1).get("id"));
        assertEquals("asd", result.get(0).get("name"));
        assertEquals("asd", result.get(1).get("name"));
        assertEquals("def", result.get(0).get("number"));
        assertEquals("def(1)", result.get(1).get("number"));

        assertEquals(1, verifyHooks.getNumOfInvocations(HookType.SAVE));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.CREATE));
        assertEquals(1, verifyHooks.getNumOfInvocations(HookType.COPY));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.UPDATE));
    }

    @Test
    public void shouldUpdateEntity() throws Exception {
        // given
        final String newName = "newName";
        final String newNumber = "newNumber";

        DataDefinition productDataDefinition = dataDefinitionService.get(PLUGIN_PRODUCTS_NAME, ENTITY_NAME_PRODUCT);

        Entity product = createProduct("asd", "def");
        product = productDataDefinition.save(product);

        verifyHooks.clear();

        // when
        product.setField("name", newName);
        product.setField("number", newNumber);
        Entity updatedProduct = productDataDefinition.save(product);

        // then
        assertEquals(newName, product.getField("name"));
        assertEquals(newNumber, product.getField("number"));
        assertNotNull(product.getId());
        assertEquals(product.getId(), updatedProduct.getId());
        assertTrue(updatedProduct.isValid());
        assertEquals(product.getDataDefinition().getName(), updatedProduct.getDataDefinition().getName());
        assertEquals(product.getDataDefinition().getPluginIdentifier(), updatedProduct.getDataDefinition().getPluginIdentifier());
        assertEquals("product", updatedProduct.getDataDefinition().getName());
        assertEquals("products", updatedProduct.getDataDefinition().getPluginIdentifier());

        Map<String, Object> result = jdbcTemplate.queryForMap("select * from " + TABLE_NAME_PRODUCT);

        assertNotNull(result);
        assertEquals(product.getId(), result.get("id"));
        assertEquals(newName, result.get("name"));
        assertEquals(newNumber, result.get("number"));

        assertEquals(1, verifyHooks.getNumOfInvocations(HookType.SAVE));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.CREATE));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.COPY));
        assertEquals(1, verifyHooks.getNumOfInvocations(HookType.UPDATE));
    }

    @Test
    public void shouldNotSaveInvalidEntity() throws Exception {
        // given
        DataDefinition productDataDefinition = dataDefinitionService.get(PLUGIN_PRODUCTS_NAME, ENTITY_NAME_PRODUCT);

        Entity product = createProduct(null, "asd");

        // when
        product = productDataDefinition.save(product);

        // then
        assertNull(product.getField("name"));
        assertNull(product.getId());
        assertFalse(product.isValid());
        assertFalse(product.isFieldValid("name"));

        int total = jdbcTemplate.queryForInt("select count(*) from " + TABLE_NAME_PRODUCT);

        assertEquals(0, total);

        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.SAVE));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.CREATE));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.COPY));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.UPDATE));
    }

    @Test
    public void shouldNotUpdateInvalidEntity() throws Exception {
        // given
        DataDefinition productDataDefinition = dataDefinitionService.get(PLUGIN_PRODUCTS_NAME, ENTITY_NAME_PRODUCT);

        Entity product = createProduct("asd", "asd");
        product.setField("quantity", "2");
        product = productDataDefinition.save(product);
        product.setField("quantity", "0");

        verifyHooks.clear();

        // when
        product = productDataDefinition.save(product);

        // then
        assertEquals(Integer.valueOf(0), Integer.valueOf(product.getField("quantity").toString()));
        assertFalse(product.isValid());
        assertFalse(product.isFieldValid("quantity"));

        Map<String, Object> result = jdbcTemplate.queryForMap("select * from " + TABLE_NAME_PRODUCT);

        assertNotNull(result);
        assertEquals(2, result.get("quantity"));

        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.SAVE));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.CREATE));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.COPY));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.UPDATE));
    }

    @Test
    public void shouldHardDeleteEntity() throws Exception {
        // given
        DataDefinition productDataDefinition = dataDefinitionService.get(PLUGIN_PRODUCTS_NAME, ENTITY_NAME_PRODUCT);

        Entity product = productDataDefinition.save(createProduct("asd", "asd"));

        verifyHooks.clear();

        // when
        productDataDefinition.delete(product.getId());

        // then
        int total = jdbcTemplate.queryForInt("select count(*) from " + TABLE_NAME_PRODUCT);

        assertEquals(0, total);

        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.SAVE));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.CREATE));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.COPY));
        assertEquals(0, verifyHooks.getNumOfInvocations(HookType.UPDATE));
    }

    @Test
    public void shouldGetEntity() throws Exception {
        // given
        DataDefinition machineDataDefinition = dataDefinitionService.get(PLUGIN_MACHINES_NAME, ENTITY_NAME_MACHINE);

        Entity savedMachine = machineDataDefinition.save(createMachine("asd"));

        // when
        Entity machine = machineDataDefinition.get(savedMachine.getId());

        // then
        assertNotNull(machine);
        assertEquals(savedMachine.getId(), machine.getId());
    }

    @Test
    public void shouldReturnNullForGettingNotExistedEntity() throws Exception {
        // given
        DataDefinition machineDataDefinition = dataDefinitionService.get(PLUGIN_MACHINES_NAME, ENTITY_NAME_MACHINE);

        // then
        Entity machine = machineDataDefinition.get(1L);

        // then
        assertNull(machine);
    }

    @Test
    public void shouldFindAllEntities() throws Exception {
        // given
        DataDefinition machineDataDefinition = dataDefinitionService.get(PLUGIN_MACHINES_NAME, ENTITY_NAME_MACHINE);

        Entity machine1 = machineDataDefinition.save(createMachine("asd"));
        Entity machine2 = machineDataDefinition.save(createMachine("def"));

        // when
        List<Entity> machines = machineDataDefinition.find().list().getEntities();

        // then
        assertNotNull(machines);
        assertEquals(2, machines.size());
        assertEquals(machine1.getId(), machines.get(0).getId());
        assertEquals(machine2.getId(), machines.get(1).getId());
    }

    @Test
    public void shouldNotFindSoftDeletedEntities() throws Exception {
        // given
        DataDefinition machineDataDefinition = dataDefinitionService.get(PLUGIN_MACHINES_NAME, ENTITY_NAME_MACHINE);

        Entity machine1 = machineDataDefinition.save(createMachine("asd"));
        Entity machine2 = machineDataDefinition.save(createMachine("def"));

        machineDataDefinition.delete(machine1.getId());

        // when
        List<Entity> machines = machineDataDefinition.find().list().getEntities();

        // then
        assertNotNull(machines);
        assertEquals(1, machines.size());
        assertEquals(machine2.getId(), machines.get(0).getId());
    }

    @Test
    public void shouldLimitEntities() throws Exception {
        // given
        DataDefinition machineDataDefinition = dataDefinitionService.get(PLUGIN_MACHINES_NAME, ENTITY_NAME_MACHINE);

        Entity machine1 = machineDataDefinition.save(createMachine("asd"));
        machineDataDefinition.save(createMachine("def"));

        // when
        List<Entity> machines = machineDataDefinition.find().setMaxResults(1).list().getEntities();

        // then
        assertNotNull(machines);
        assertEquals(1, machines.size());
        assertEquals(machine1.getId(), machines.get(0).getId());
    }

    @Test
    public void shouldOffsetEntities() throws Exception {
        // given
        DataDefinition machineDataDefinition = dataDefinitionService.get(PLUGIN_MACHINES_NAME, ENTITY_NAME_MACHINE);

        machineDataDefinition.save(createMachine("asd"));
        Entity machine2 = machineDataDefinition.save(createMachine("def"));

        // when
        List<Entity> machines = machineDataDefinition.find().setFirstResult(1).setMaxResults(1).list().getEntities();

        // then
        assertNotNull(machines);
        assertEquals(1, machines.size());
        assertEquals(machine2.getId(), machines.get(0).getId());
    }

    @Test
    public void shouldOrderAscEntities() throws Exception {
        // given
        DataDefinition machineDataDefinition = dataDefinitionService.get(PLUGIN_MACHINES_NAME, ENTITY_NAME_MACHINE);

        Entity machine1 = machineDataDefinition.save(createMachine("asd"));
        Entity machine2 = machineDataDefinition.save(createMachine("def"));

        // when
        List<Entity> machines = machineDataDefinition.find().addOrder(SearchOrders.desc("name")).list().getEntities();

        // then
        assertNotNull(machines);
        assertEquals(2, machines.size());
        assertEquals(machine2.getId(), machines.get(0).getId());
        assertEquals(machine1.getId(), machines.get(1).getId());
    }

    @Test
    public void shouldOrderDescEntities() throws Exception {
        // given
        DataDefinition machineDataDefinition = dataDefinitionService.get(PLUGIN_MACHINES_NAME, ENTITY_NAME_MACHINE);

        Entity machine1 = machineDataDefinition.save(createMachine("asd"));
        Entity machine2 = machineDataDefinition.save(createMachine("def"));

        // when
        List<Entity> machines = machineDataDefinition.find().addOrder(SearchOrders.asc("name")).list().getEntities();

        // then
        assertNotNull(machines);
        assertEquals(2, machines.size());
        assertEquals(machine1.getId(), machines.get(0).getId());
        assertEquals(machine2.getId(), machines.get(1).getId());
    }

    @Test
    public void shouldOrderEntitiesByMultipleFields() throws Exception {
        // given
        DataDefinition productDataDefinition = dataDefinitionService.get(PLUGIN_PRODUCTS_NAME, ENTITY_NAME_PRODUCT);

        Entity product1 = productDataDefinition.save(createProduct("asd", "asd"));
        Entity product2 = productDataDefinition.save(createProduct("def", "asd"));
        Entity product3 = productDataDefinition.save(createProduct("def", "def"));

        // when
        List<Entity> products = productDataDefinition.find().addOrder(SearchOrders.desc("name"))
                .addOrder(SearchOrders.asc("number")).list().getEntities();

        // then
        assertNotNull(products);
        assertEquals(2, products.size());
        assertFalse(product2.isValid());
        assertEquals(product3.getId(), products.get(0).getId());
        assertEquals(product1.getId(), products.get(1).getId());
    }

    @Test
    public void shouldUseEqualsRestriction() throws Exception {
        // given
        DataDefinition machineDataDefinition = dataDefinitionService.get(PLUGIN_MACHINES_NAME, ENTITY_NAME_MACHINE);

        machineDataDefinition.save(createMachine("asd"));
        Entity machine2 = machineDataDefinition.save(createMachine("def"));

        // when
        List<Entity> machines = machineDataDefinition.find().add(SearchRestrictions.eq("name", "def")).list().getEntities();

        // then
        assertNotNull(machines);
        assertEquals(1, machines.size());
        assertEquals(machine2.getId(), machines.get(0).getId());
    }

    @Test
    public void shouldUseOrRestriction() throws Exception {
        // given
        DataDefinition machineDataDefinition = dataDefinitionService.get(PLUGIN_MACHINES_NAME, ENTITY_NAME_MACHINE);

        machineDataDefinition.save(createMachine("asd"));
        machineDataDefinition.save(createMachine("def"));

        // when
        List<Entity> entities = machineDataDefinition.find()
                .add(SearchRestrictions.or(SearchRestrictions.eq("name", "def"), SearchRestrictions.eq("name", "asd"))).list()
                .getEntities();

        // then
        assertNotNull(entities);
        assertEquals(2, entities.size());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWhileSavingIncompatibleType() throws Exception {
        // given
        DataDefinition machineDataDefinition = dataDefinitionService.get(PLUGIN_MACHINES_NAME, ENTITY_NAME_MACHINE);
        Entity product = createProduct("name", "number");

        // when
        machineDataDefinition.save(product);
    }

    @Test(expected = NullPointerException.class)
    public void shouldFailWhileSavingNullEntity() throws Exception {
        // given
        DataDefinition machineDataDefinition = dataDefinitionService.get(PLUGIN_MACHINES_NAME, ENTITY_NAME_MACHINE);

        // when
        machineDataDefinition.save(null);
    }

}
