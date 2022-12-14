/*
 * Copyright 2022 Ververica Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.cdc.connectors.mysql.table;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.planner.factories.TestValuesTableFactory;
import org.apache.flink.types.Row;
import org.apache.flink.util.CloseableIterator;

import com.ververica.cdc.connectors.mysql.source.MySqlSourceTestBase;
import com.ververica.cdc.connectors.mysql.testutils.UniqueDatabase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Test supporting different column charsets for MySQL Table source. */
@RunWith(Parameterized.class)
public class MysqlConnectorCharsetITCase extends MySqlSourceTestBase {

    private static final String TEST_USER = "mysqluser";
    private static final String TEST_PASSWORD = "mysqlpw";

    private static final UniqueDatabase charsetTestDatabase =
            new UniqueDatabase(MYSQL_CONTAINER, "charset_test", TEST_USER, TEST_PASSWORD);

    private final StreamExecutionEnvironment env =
            StreamExecutionEnvironment.getExecutionEnvironment();

    private final StreamTableEnvironment tEnv =
            StreamTableEnvironment.create(
                    env, EnvironmentSettings.newInstance().inStreamingMode().build());

    private final String testName;
    private final String[] snapshotExpected;
    private final String[] binlogExpected;

    public MysqlConnectorCharsetITCase(
            String testName, String[] snapshotExpected, String[] binlogExpected) {
        this.testName = testName;
        this.snapshotExpected = snapshotExpected;
        this.binlogExpected = binlogExpected;
    }

    @Parameterized.Parameters(name = "Test column charset: {0}")
    public static Object[] parameters() {
        return new Object[][] {
            new Object[] {
                "ucs2_test",
                new String[] {"+I[1, ????????????]", "+I[2, Craig Marshall]", "+I[3, ?????????????????????]"},
                new String[] {
                    "-D[1, ????????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ?????????????????????]",
                    "+I[11, ????????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ?????????????????????]"
                }
            },
            new Object[] {
                "utf8_test",
                new String[] {"+I[1, ????????????]", "+I[2, Craig Marshall]", "+I[3, ?????????????????????]"},
                new String[] {
                    "-D[1, ????????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ?????????????????????]",
                    "+I[11, ????????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ?????????????????????]"
                }
            },
            new Object[] {
                "ascii_test",
                new String[] {"+I[1, ascii test!?]", "+I[2, Craig Marshall]", "+I[3, {test}]"},
                new String[] {
                    "-D[1, ascii test!?]",
                    "-D[2, Craig Marshall]",
                    "-D[3, {test}]",
                    "+I[11, ascii test!?]",
                    "+I[12, Craig Marshall]",
                    "+I[13, {test}]"
                }
            },
            new Object[] {
                "sjis_test",
                new String[] {"+I[1, ?????????]", "+I[2, Craig Marshall]", "+I[3, ?????????]"},
                new String[] {
                    "-D[1, ?????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ?????????]",
                    "+I[11, ?????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ?????????]"
                }
            },
            new Object[] {
                "gbk_test",
                new String[] {"+I[1, ????????????]", "+I[2, Craig Marshall]", "+I[3, ?????????????????????]"},
                new String[] {
                    "-D[1, ????????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ?????????????????????]",
                    "+I[11, ????????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ?????????????????????]"
                }
            },
            new Object[] {
                "cp932_test",
                new String[] {"+I[1, ?????????]", "+I[2, Craig Marshall]", "+I[3, ?????????]"},
                new String[] {
                    "-D[1, ?????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ?????????]",
                    "+I[11, ?????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ?????????]"
                }
            },
            new Object[] {
                "gb2312_test",
                new String[] {"+I[1, ????????????]", "+I[2, Craig Marshall]", "+I[3, ?????????????????????]"},
                new String[] {
                    "-D[1, ????????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ?????????????????????]",
                    "+I[11, ????????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ?????????????????????]"
                }
            },
            new Object[] {
                "ujis_test",
                new String[] {"+I[1, ?????????]", "+I[2, Craig Marshall]", "+I[3, ?????????]"},
                new String[] {
                    "-D[1, ?????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ?????????]",
                    "+I[11, ?????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ?????????]"
                }
            },
            new Object[] {
                "euckr_test",
                new String[] {"+I[1, ?????????]", "+I[2, Craig Marshall]", "+I[3, ?????????]"},
                new String[] {
                    "-D[1, ?????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ?????????]",
                    "+I[11, ?????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ?????????]"
                }
            },
            new Object[] {
                "latin1_test",
                new String[] {"+I[1, ??????]", "+I[2, Craig Marshall]", "+I[3, ??????]"},
                new String[] {
                    "-D[1, ??????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ??????]",
                    "+I[11, ??????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ??????]"
                }
            },
            new Object[] {
                "latin2_test",
                new String[] {"+I[1, ????????]", "+I[2, Craig Marshall]", "+I[3, ????????]"},
                new String[] {
                    "-D[1, ????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ????????]",
                    "+I[11, ????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ????????]"
                }
            },
            new Object[] {
                "greek_test",
                new String[] {"+I[1, ??????????]", "+I[2, Craig Marshall]", "+I[3, ????????]"},
                new String[] {
                    "-D[1, ??????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ????????]",
                    "+I[11, ??????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ????????]"
                }
            },
            new Object[] {
                "hebrew_test",
                new String[] {"+I[1, ??????????]", "+I[2, Craig Marshall]", "+I[3, ????????]"},
                new String[] {
                    "-D[1, ??????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ????????]",
                    "+I[11, ??????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ????????]"
                }
            },
            new Object[] {
                "cp866_test",
                new String[] {"+I[1, ????????]", "+I[2, Craig Marshall]", "+I[3, ??????????]"},
                new String[] {
                    "-D[1, ????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ??????????]",
                    "+I[11, ????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ??????????]"
                }
            },
            new Object[] {
                "tis620_test",
                new String[] {"+I[1, ?????????????????????]", "+I[2, Craig Marshall]", "+I[3, ????????????]"},
                new String[] {
                    "-D[1, ?????????????????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ????????????]",
                    "+I[11, ?????????????????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ????????????]"
                }
            },
            new Object[] {
                "cp1250_test",
                new String[] {"+I[1, ????????]", "+I[2, Craig Marshall]", "+I[3, ????????]"},
                new String[] {
                    "-D[1, ????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ????????]",
                    "+I[11, ????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ????????]"
                }
            },
            new Object[] {
                "cp1251_test",
                new String[] {"+I[1, ????????]", "+I[2, Craig Marshall]", "+I[3, ??????????]"},
                new String[] {
                    "-D[1, ????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ??????????]",
                    "+I[11, ????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ??????????]"
                }
            },
            new Object[] {
                "cp1257_test",
                new String[] {
                    "+I[1, piedzimst br??vi]", "+I[2, Craig Marshall]", "+I[3, apvelt??ti ar sapr??tu]"
                },
                new String[] {
                    "-D[1, piedzimst br??vi]", "-D[2, Craig Marshall]",
                            "-D[3, apvelt??ti ar sapr??tu]",
                    "+I[11, piedzimst br??vi]", "+I[12, Craig Marshall]",
                            "+I[13, apvelt??ti ar sapr??tu]"
                }
            },
            new Object[] {
                "macroman_test",
                new String[] {"+I[1, ??????]", "+I[2, Craig Marshall]", "+I[3, ??????]"},
                new String[] {
                    "-D[1, ??????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ??????]",
                    "+I[11, ??????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ??????]"
                }
            },
            new Object[] {
                "macce_test",
                new String[] {"+I[1, ????????]", "+I[2, Craig Marshall]", "+I[3, ????????]"},
                new String[] {
                    "-D[1, ????????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ????????]",
                    "+I[11, ????????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ????????]"
                }
            },
            new Object[] {
                "big5_test",
                new String[] {"+I[1, ??????]", "+I[2, Craig Marshall]", "+I[3, ??????]"},
                new String[] {
                    "-D[1, ??????]",
                    "-D[2, Craig Marshall]",
                    "-D[3, ??????]",
                    "+I[11, ??????]",
                    "+I[12, Craig Marshall]",
                    "+I[13, ??????]"
                }
            }
        };
    }

    @BeforeClass
    public static void beforeClass() {
        charsetTestDatabase.createAndInitialize();
    }

    @Before
    public void before() {
        TestValuesTableFactory.clearAllData();
        env.setParallelism(DEFAULT_PARALLELISM);
        env.enableCheckpointing(200);
    }

    @Test
    public void testCharset() throws Exception {
        String sourceDDL =
                String.format(
                        "CREATE TABLE %s (\n"
                                + "  table_id BIGINT,\n"
                                + "  table_name STRING,\n"
                                + "  primary key(table_id) not enforced"
                                + ") WITH ("
                                + " 'connector' = 'mysql-cdc',"
                                + " 'hostname' = '%s',"
                                + " 'port' = '%s',"
                                + " 'username' = '%s',"
                                + " 'password' = '%s',"
                                + " 'database-name' = '%s',"
                                + " 'table-name' = '%s',"
                                + " 'scan.incremental.snapshot.enabled' = '%s',"
                                + " 'server-id' = '%s',"
                                + " 'server-time-zone' = 'UTC',"
                                + " 'scan.incremental.snapshot.chunk.size' = '%s'"
                                + ")",
                        testName,
                        MYSQL_CONTAINER.getHost(),
                        MYSQL_CONTAINER.getDatabasePort(),
                        charsetTestDatabase.getUsername(),
                        charsetTestDatabase.getPassword(),
                        charsetTestDatabase.getDatabaseName(),
                        testName,
                        true,
                        getServerId(),
                        4);
        tEnv.executeSql(sourceDDL);
        // async submit job
        TableResult result =
                tEnv.executeSql(String.format("SELECT table_id,table_name FROM %s", testName));

        // test snapshot phase
        CloseableIterator<Row> iterator = result.collect();
        waitForSnapshotStarted(iterator);
        assertEqualsInAnyOrder(
                Arrays.asList(snapshotExpected), fetchRows(iterator, snapshotExpected.length));

        // test binlog phase
        try (Connection connection = charsetTestDatabase.getJdbcConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(String.format("UPDATE %s SET table_id = table_id + 10;", testName));
        }
        assertEqualsInAnyOrder(
                Arrays.asList(binlogExpected), fetchRows(iterator, binlogExpected.length));
        result.getJobClient().ifPresent(client -> client.cancel());
    }

    private String getServerId() {
        final Random random = new Random();
        int serverId = random.nextInt(100) + 5400;
        return serverId + "-" + (serverId + env.getParallelism());
    }

    private static List<String> fetchRows(Iterator<Row> iter, int size) {
        List<String> rows = new ArrayList<>(size);
        while (size > 0 && iter.hasNext()) {
            Row row = iter.next();
            rows.add(row.toString());
            size--;
        }
        return rows;
    }

    private static void waitForSnapshotStarted(CloseableIterator<Row> iterator) throws Exception {
        while (!iterator.hasNext()) {
            Thread.sleep(100);
        }
    }
}
