package com.qualificamaisanaliticinterface.features.people;

import com.qualificamaisanaliticinterface.features.enuns.Disabilities;
import com.qualificamaisanaliticinterface.features.enuns.Education;
import com.qualificamaisanaliticinterface.features.enuns.Gender;
import com.qualificamaisanaliticinterface.features.enuns.WorkState;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static com.qualificamaisanaliticinterface.features.people.PersonTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class PersonControllersTest {
    @BeforeAll static void startJavaFx() { startFx(); }

    @Test
    void newPersonFormOffersAllEnumChoices() throws Exception {
        fx(() -> {
            PersonFormController controller = load("person-form.fxml");
            assertTrue(controller.genderField.getItems().containsAll(Arrays.asList(Gender.values())));
            assertTrue(controller.educationField.getItems().containsAll(Arrays.asList(Education.values())));
            assertTrue(controller.workstateField.getItems().containsAll(Arrays.asList(WorkState.values())));
            assertTrue(controller.disabilitiesField.getItems().containsAll(Arrays.asList(Disabilities.values())));
            return null;
        });
    }

    @Test
    void personInfoDisplaysProvidedData() throws Exception {
        fx(() -> {
            PersonInfoController controller = load("person-info.fxml");
            controller.getPersonInfo(person());
            assertEquals("Ana Silva", controller.infoName.getText());
            assertEquals("Ana", controller.socialNameField.getText());
            assertEquals("12345678901", controller.cpfField.getText());
            assertEquals("ana@example.com", controller.emailField.getText());
            assertTrue(controller.hasWhatsAppCheckBox.isSelected());
            assertEquals("42", controller.numberField.getText());
            assertEquals("Rua das Flores", controller.streetField.getText());
            assertEquals(Gender.FEMALE.toString(), controller.genderField.getText());
            assertEquals(Education.HIGH_SCHOOL_COMPLETE.toString(), controller.educationField.getText());
            assertEquals(WorkState.ONLY_STUDYING.toString(), controller.workstateField.getText());
            assertTrue(controller.disabilitiesInfoField.getText().contains(Disabilities.HEARING.toString()));
            return null;
        });
    }

    @Test
    void updatePersonPopulatesFieldsAndDisabilities() throws Exception {
        fx(() -> {
            PersonUpdateController controller = load("person-update.fxml");
            controller.setPerson(person());
            assertEquals("Ana Silva", controller.nameField.getText());
            assertEquals("12345678901", controller.cpfField.getText());
            assertEquals("ana@example.com", controller.emailField.getText());
            assertEquals("42", controller.numberField.getText());
            assertEquals("Centro", controller.neighborhoodField.getText());
            assertTrue(controller.hasWhatsAppCheckBox.isSelected());
            assertEquals(Gender.FEMALE, controller.genderField.getValue());
            assertEquals(Set.of(Disabilities.HEARING), Set.copyOf(controller.disabilitiesField.getCheckModel().getCheckedItems()));
            return null;
        });
    }

    @Test
    void updatePersonHandlesMissingOptionalFields() throws Exception {
        fx(() -> {
            PersonUpdateController controller = load("person-update.fxml");
            PersonDto sparse = new PersonDto(8L, "Bruno Lima", null, "98765432100", null,
                    null, null, null, null, null, null, null, null);
            controller.setPerson(sparse);
            assertEquals("", controller.socialNameField.getText());
            assertEquals("", controller.emailField.getText());
            assertEquals("", controller.numberField.getText());
            assertEquals("", controller.streetField.getText());
            assertFalse(controller.hasWhatsAppCheckBox.isSelected());
            assertTrue(controller.disabilitiesField.getCheckModel().getCheckedItems().isEmpty());
            return null;
        });
    }

    @Test
    void updatePersonRequiresSelectionBeforeSave() throws Exception {
        fx(() -> {
            PersonUpdateController controller = load("person-update.fxml");
            controller.save.fire();
            assertEquals("Selecione uma pessoa antes de salvar.", controller.info.getText());
            assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
    }

    @Test
    void updatePersonRejectsNonNumericAddressBeforeCallingApi() throws Exception {
        fx(() -> {
            PersonUpdateController controller = load("person-update.fxml");
            controller.setPerson(person());
            controller.numberField.setText("12A");
            controller.save.fire();
            assertEquals("Informe um número de endereço inteiro válido.", controller.info.getText());
            assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
    }

    @Test
    void deleteConfirmationShowsSelectedPerson() throws Exception {
        fx(() -> {
            PersonDeleteController controller = load("person-delete.fxml");
            controller.getDelectInfo(person());
            assertEquals(7L, controller.personId);
            assertEquals("Ana Silva", controller.deleteName.getText());
            return null;
        });
    }
}
