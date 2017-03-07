package org.spine3.gradle.protobuf;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import org.spine3.change.Fixed32Change;
import org.spine3.change.Fixed64Change;
import org.spine3.change.SInt32Change;
import org.spine3.change.SInt64Change;
import org.spine3.change.Sfixed32Change;
import org.spine3.change.Sfixed64Change;

import java.util.Map;

public class GenerationUtils {

    private GenerationUtils() {
        //Prevent instantiation.
    }

    // https://developers.google.com/protocol-buffers/docs/proto3#scalar
    @SuppressWarnings({"DuplicateStringLiteralInspection", "ConstantConditions"})
    private static final Map<String, Class<?>> PROTO_FIELD_TYPES = ImmutableMap.<String, Class<?>>builder()
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE.name(), double.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT.name(), float.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64.name(), long.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64.name(), long.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32.name(), int.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64.name(), long.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32.name(), int.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL.name(), boolean.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING.name(), String.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES.name(), ByteString.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32.name(), int.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32.name(), int.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64.name(), long.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32.name(), int.class)
            .put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64.name(), int.class)

            /*
             * Groups are NOT supported, so do not create an associated Java type for it.
             * The return value for the {@link FieldDescriptorProto.Type.TYPE_GROUP} key
             * is intended to be {@code null}.
             **/
            //.put(FieldDescriptorProto.Type.TYPE_GROUP.name(), "not supported")

            .build();

    public static Class<?> getType(String type) {
        return PROTO_FIELD_TYPES.get(type);
    }

    /**
     * Transforms Protobuf-style field name into corresponding Java-style field name.
     *
     * <p>For example, seat_assignment_id -> SeatAssignmentId
     *
     * @param protoFieldName  Protobuf field name.
     * @param capitalizeFirst Indicates if we need first letter of the output to be capitalized.
     * @return a field name
     */
    public static String getJavaFieldName(String protoFieldName, boolean capitalizeFirst) {
        final String[] words = protoFieldName.split("_");
        final StringBuilder builder = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            final String word = words[i];
            builder.append(Character.toUpperCase(word.charAt(0)))
                   .append(word.substring(1));
        }
        String resultName = builder.toString();
        if (capitalizeFirst) {
            resultName = Character.toUpperCase(resultName.charAt(0)) + resultName.substring(1);
        }
        return resultName;
    }
}
