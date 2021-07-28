package com.veeva.vault.custom.fmc;

import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;

@UserDefinedClassInfo
public class Constants {

    public static final String ID_FIELD_NAME = "id";

    public interface Record {
        String ID = "id";
        String NAME = "name__v";
        String OBJECT_TYPE = "object_type__v";
    }

    public interface Formulation {
        String NAME = "formulation__v";

        interface Field {
            String TITLE = "title__v";
            String VERSION_SOURCE = "version_source__v";
            String LOCATION = "location__c";
            String VERSION = "version_number__v";
            String ORGANIZATION = "organization__c";
            String PREVIOUS_VERSION = "previous_version__c";
            String EXTERNAL_ID = "external_id__v";
        }

        interface Relationship {
            String VERSION_SOURCE_TITLE = "version_source__vr.title__v";
            String VERSION_SOURCE_EXTERNAL_ID = "version_source__vr.external_id__v";
            String COMPOSITIONS = "child_compositions__vr";
            String COMPOSITIONS2 = "formulation_compositions1__cr";
            String GROUPINGS = "groupings__cr";
        }

        interface ObjectType {
            String REGISTERED_SPECIFICATION = "registered_specification__c";
            String FIVE_BATCH_RESULT = "5_batch_result__c";
        }
    }

    public interface FormulationCountryJoin {
        String NAME = "formulation_country_join__c";

        interface Field {
            String CREATE_COUNTRY_REGISTRATION = "create_country_registration__c";
            String SPEC = "spec__c";
            String LOCATION = "location__c";
        }
    }

    public interface Grouping {
        String NAME = "grouping__c";

        interface Field {
            String FORMULATION = "formulation__c";
        }
    }

    public interface Composition {
        String NAME = "formulation_composition__v";

        interface Field {
            String CHILD = "child__v";
            String PARENT = "parent__v";
            String SPEC = "spec__c";
            String MIN = "min__c";
            String MAX = "max__c";
            String TARGET = "target__c";
            String QUANTITY = "quantity__c";
            String GROUPING = "grouping__c";
            String PURPOSE = "purpose__c";
            String AVERAGE_PERCENT_WEIGHT = "average_weight__c";
            String AVERAGE_PLUS_MINUS_3_SD = "average_3_sd__c";
            String ORDER = "order__c";
        }

        interface ObjectType {
            String FIVE_BATCH = "5_batch__c";
        }
    }

    public interface Comparison {
        String NAME = "comparison__c";

        interface Field {
            String FORMULATION_1 = "formulation_1__c";
            String FORMULATION_2 = "formulation_2__c";
            String FORMULATION_1_TYPE = "formulation_1_type__c";
            String FORMULATION_2_TYPE = "formulation_2_type__c";
        }
    }

    public interface ComparisonResult {
        String NAME = "comparison_result__c";

        interface Field {
            String VALUE_1 = "value_1__c";
            String VALUE_2 = "value_2__c";
            String MATCH = "match__c";
            String FORMULATION = "formulation__c";
            String COMPARISON = "comparison__c";
        }
    }

    public interface CustomSetting {
        String DEEP_COPY_EXTERNAL_ID = "dc";
        String ID_FIELD = "idField";
        String FIELDS = "fields";
        String COMPARISON_EXTERNAL_ID = "comp";
        String OT_FIELD_MAPPINGS = "OTFieldMappings";
        String IS_NULL = "isNull";
        String OBJECT = "object";
        String CREATE_COUNTRY_REGISTRATION_EXT_ID = "ccr";
        String OBJECT_TYPE = "objectType";
    }

    public interface Purpose {
        String NAME = "purpose__c";

        interface PicklistValue {
            String ACTIVE_INGREDIENT = "active_ingredient__c";
            String ACT_AGENT = "act_agent__c";
        }
    }

    private Constants() {
    }
}