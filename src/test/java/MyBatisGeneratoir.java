import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.sql.Types;
import java.util.Collections;

public class MyBatisGeneratoir {

    public static void main(String[] args) {
        String URL="jdbc:mysql://localhost:3306/online_exam?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2b8";

        FastAutoGenerator.create(URL, "root", "root123456")
                .globalConfig(builder -> {
                    builder.author("joker") // 设置作者
                            .outputDir("/Users/qyk9527/ideaProject/exam_system_online/src/main/java"); // 指定输出目录
                })
                .dataSourceConfig(builder -> builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                    int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                    if (typeCode == Types.SMALLINT) {
                        // 自定义类型转换
                        return DbColumnType.INTEGER;
                    }
                    return typeRegistry.getColumnType(metaInfo);

                }))
                .packageConfig(builder -> {
                    builder.parent("com.joker.ai") // 设置父包名
                            .moduleName("exam") // 设置父包模块名
                            .pathInfo(Collections.singletonMap(OutputFile.mapper, "/Users/qyk9527/ideaProject/exam_system_online/src/main/java/com/joker/ai/exam/mapper"))
                            .pathInfo(Collections.singletonMap(OutputFile.controller, "/Users/qyk9527/ideaProject/exam_system_online/src/main/java/com/joker/ai/exam/controller"))
                            .pathInfo(Collections.singletonMap(OutputFile.service, "/Users/qyk9527/ideaProject/exam_system_online/src/main/java/com/joker/ai/exam/service"))
                            .pathInfo(Collections.singletonMap(OutputFile.serviceImpl, "/Users/qyk9527/ideaProject/exam_system_online/src/main/java/com/joker/ai/exam/service/impl"))
                            .pathInfo(Collections.singletonMap(OutputFile.entity, "/Users/qyk9527/ideaProject/exam_system_online/src/main/java/entity"))
                            .pathInfo(Collections.singletonMap(OutputFile.xml, "/Users/qyk9527/ideaProject/exam_system_online/src/main/resources/mapper")); // 设置mapperXml生成路径
                })
                .strategyConfig(builder -> {
                    builder.addExclude("") // 设置需要生成的表名
                            .addTablePrefix("t_", "c_"); // 设置过滤表前缀
                })
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();
    }

}