// SPDX-FileCopyrightText: 2023 LakeSoul Contributors
//
// SPDX-License-Identifier: Apache-2.0

package com.facebook.presto.lakesoul.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dmetasoul.lakesoul.meta.DataFileInfo;
import com.dmetasoul.lakesoul.meta.DataOperation;
import com.dmetasoul.lakesoul.meta.LakeSoulOptions;
import com.dmetasoul.lakesoul.meta.entity.TableInfo;
import com.facebook.presto.common.type.*;
import com.facebook.presto.lakesoul.pojo.Path;
import com.facebook.presto.lakesoul.type.FloatType;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.ArrowType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrestoUtil {

    public static boolean isExistHashPartition(TableInfo tif) {
        JSONObject tableProperties = JSON.parseObject(tif.getProperties());
        if (tableProperties.containsKey(LakeSoulOptions.HASH_BUCKET_NUM()) && tableProperties.getString(LakeSoulOptions.HASH_BUCKET_NUM()).equals("-1")) {
            return false;
        } else {
            return tableProperties.containsKey(LakeSoulOptions.HASH_BUCKET_NUM());
        }
    }

    public static Map<String, Map<Integer, List<Path>>> splitDataInfosToRangeAndHashPartition(String tid, DataFileInfo[] dfinfos) {
        Map<String, Map<Integer, List<Path>>> splitByRangeAndHashPartition = new LinkedHashMap<>();
        TableInfo tif = DataOperation.dbManager().getTableInfoByTableId(tid);
        for (DataFileInfo pif : dfinfos) {
            if (isExistHashPartition(tif) && pif.file_bucket_id() != -1) {
                splitByRangeAndHashPartition.computeIfAbsent(pif.range_partitions(), k -> new LinkedHashMap<>())
                        .computeIfAbsent(pif.file_bucket_id(), v -> new ArrayList<>())
                        .add(new Path(pif.path()));
            } else {
                splitByRangeAndHashPartition.computeIfAbsent(pif.range_partitions(), k -> new LinkedHashMap<>())
                        .computeIfAbsent(-1, v -> new ArrayList<>())
                        .add(new Path(pif.path()));
            }
        }
        return splitByRangeAndHashPartition;
    }

    /**
     * conver arrow type to presto type
     * @param type arrow type
     * @return presto type
     */
    public static Type convertToPrestoType(ArrowType type){
        return type.accept(ArrowTypeToLogicalTypeConverter.INSTANCE);
    }

    /**
     * a converter for arrow field
     */
    private static class ArrowTypeToLogicalTypeConverter
            implements ArrowType.ArrowTypeVisitor<Type> {

        private static final ArrowTypeToLogicalTypeConverter INSTANCE =
                new ArrowTypeToLogicalTypeConverter();

        @Override
        public Type visit(ArrowType.Null type) {
            return UnknownType.UNKNOWN;
        }

        @Override
        public Type visit(ArrowType.Struct type) {
            return UnknownType.UNKNOWN;
        }

        @Override
        public Type visit(ArrowType.List type) {
            return UnknownType.UNKNOWN;
        }

        @Override
        public Type visit(ArrowType.LargeList type) {
            return UnknownType.UNKNOWN;
        }

        @Override
        public Type visit(ArrowType.FixedSizeList type) {
            return UnknownType.UNKNOWN;
        }

        @Override
        public Type visit(ArrowType.Union type) {
            return UnknownType.UNKNOWN;
        }

        @Override
        public Type visit(ArrowType.Map type) {
            return UnknownType.UNKNOWN;
        }

        @Override
        public Type visit(ArrowType.Int type) {
            if(type.getBitWidth() == 64){
                return BigintType.BIGINT;
            }else if(type.getBitWidth() == 32){
                return IntegerType.INTEGER;
            }else if(type.getBitWidth() == 16){
                return SmallintType.SMALLINT;
            }else if(type.getBitWidth() == 8){
                return TinyintType.TINYINT;
            }
            return BigintType.BIGINT;
        }

        @Override
        public Type visit(ArrowType.FloatingPoint type) {
            if(type.getPrecision() == FloatingPointPrecision.HALF){
                return UnknownType.UNKNOWN;
            }else if(type.getPrecision() == FloatingPointPrecision.SINGLE){
                //return FloatType.FLOAT;
                return DoubleType.DOUBLE;
            }else if(type.getPrecision() == FloatingPointPrecision.DOUBLE) {
                return DoubleType.DOUBLE;
            }
            return DoubleType.DOUBLE;
        }

        @Override
        public Type visit(ArrowType.Utf8 type) {
            return VarcharType.VARCHAR;
        }

        @Override
        public Type visit(ArrowType.LargeUtf8 type) {
            return VarcharType.VARCHAR;
        }

        @Override
        public Type visit(ArrowType.Binary type) {
            return VarbinaryType.VARBINARY;
        }

        @Override
        public Type visit(ArrowType.LargeBinary type) {
            return VarbinaryType.VARBINARY;
        }

        @Override
        public Type visit(ArrowType.FixedSizeBinary type) {
            return VarbinaryType.VARBINARY;
        }

        @Override
        public Type visit(ArrowType.Bool type) {
            return BooleanType.BOOLEAN;
        }

        @Override
        public Type visit(ArrowType.Decimal type) {
            return DecimalType.createDecimalType(type.getPrecision(), type.getScale());
        }

        @Override
        public Type visit(ArrowType.Date type) {
            return DateType.DATE;
        }

        @Override
        public Type visit(ArrowType.Time type) {
            return TimeType.TIME;
        }

        @Override
        public Type visit(ArrowType.Timestamp type) {
            return TimestampType.TIMESTAMP;
        }

        @Override
        public Type visit(ArrowType.Interval type) {
            return UnknownType.UNKNOWN;
        }

        @Override
        public Type visit(ArrowType.Duration type) {
            return UnknownType.UNKNOWN;
        }
    }


}
