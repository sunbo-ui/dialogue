package com.mybb.dialogue;

import java.util.List;

public class Entity {
    public String status;
    public OMLData data;

    @Override
    public String toString() {
        return "Entity{" +
                "status='" + status + '\'' +
                ", data=" + data +
                '}';
    }

    public class OMLData {
        public List<Nli> nli;

        public class Nli {
            public DescObj desc_obj;
            public List<DataObj> data_obj;
            public String type;

            public  class DescObj {
                public String result;
                public Integer status;
            }

            public class DataObj {
                public List<Integer> highlight;
                public List<String> field_value;
                public String description;
                public String photo_url;
                public String type;
                public List<String> categorylist;
                public List<String> field_name;

                @Override
                public String toString() {
                    return "DataObj{" +
                            "highlight=" + highlight +
                            ", field_value=" + field_value +
                            ", description='" + description + '\'' +
                            ", photo_url='" + photo_url + '\'' +
                            ", type='" + type + '\'' +
                            ", categorylist=" + categorylist +
                            ", field_name=" + field_name +
                            '}';
                }
            }
        }
    }

}
