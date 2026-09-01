package com.victorhugo.boleiragem.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.victorhugo.boleiragem.data.dao.ConfiguracaoDao
import com.victorhugo.boleiragem.data.dao.ConfiguracaoPontuacaoDao
import com.victorhugo.boleiragem.data.dao.GrupoPeladaDao
import com.victorhugo.boleiragem.data.dao.HistoricoPeladaDao
import com.victorhugo.boleiragem.data.dao.HistoricoTimeDao
import com.victorhugo.boleiragem.data.dao.JogadorDao
import com.victorhugo.boleiragem.data.model.ConfiguracaoPontuacao
import com.victorhugo.boleiragem.data.model.ConfiguracaoSorteio
import com.victorhugo.boleiragem.data.model.GrupoPelada
import com.victorhugo.boleiragem.data.model.HistoricoPelada
import com.victorhugo.boleiragem.data.model.HistoricoTime
import com.victorhugo.boleiragem.data.model.Jogador
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        Jogador::class,
        ConfiguracaoSorteio::class,
        HistoricoTime::class,
        ConfiguracaoPontuacao::class,
        HistoricoPelada::class,
        GrupoPelada::class
    ],
    version = 14, // Incrementado de 13 para 14 para o vínculo com o Firestore (firestoreId)
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BoleiragemDatabase : RoomDatabase() {
    abstract fun jogadorDao(): JogadorDao
    abstract fun configuracaoDao(): ConfiguracaoDao
    abstract fun historicoTimeDao(): HistoricoTimeDao
    abstract fun configuracaoPontuacaoDao(): ConfiguracaoPontuacaoDao
    abstract fun historicoPeladaDao(): HistoricoPeladaDao
    abstract fun grupoPeladaDao(): GrupoPeladaDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Criar a nova tabela historico_time com a estrutura correta
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `historico_time` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nome` TEXT NOT NULL,
                        `vitorias` INTEGER NOT NULL,
                        `derrotas` INTEGER NOT NULL,
                        `empates` INTEGER NOT NULL,
                        `dataUltimoSorteio` INTEGER NOT NULL,
                        `jogadoresIds` TEXT NOT NULL
                    )
                    """
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Adicionar novas colunas na tabela jogadores
                database.execSQL("ALTER TABLE jogadores ADD COLUMN totalJogos INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE jogadores ADD COLUMN vitorias INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE jogadores ADD COLUMN derrotas INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE jogadores ADD COLUMN empates INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE jogadores ADD COLUMN pontuacaoTotal INTEGER NOT NULL DEFAULT 0")

                // 2. Criar a tabela de configuração de pontuação
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `configuracao_pontuacao` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `pontosPorVitoria` INTEGER NOT NULL,
                        `pontosPorDerrota` INTEGER NOT NULL,
                        `pontosPorEmpate` INTEGER NOT NULL
                    )
                    """
                )

                // 3. Inserir a configuração de pontuação padrão
                database.execSQL(
                    """
                    INSERT INTO `configuracao_pontuacao` (`id`, `pontosPorVitoria`, `pontosPorDerrota`, `pontosPorEmpate`)
                    VALUES (1, 10, -10, -5)
                    """
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Criar uma tabela temporária com a nova estrutura
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `configuracao_sorteio_temp` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `qtdJogadoresPorTime` INTEGER NOT NULL,
                        `qtdTimes` INTEGER NOT NULL,
                        `aleatorio` INTEGER NOT NULL DEFAULT 1,
                        `criteriosExtras` TEXT NOT NULL DEFAULT '[]'
                    )
                    """
                )

                // 2. Copiar os dados da tabela antiga para a nova,
                //    assumindo que aleatorio é true (1) por padrão
                database.execSQL(
                    """
                    INSERT INTO configuracao_sorteio_temp (id, qtdJogadoresPorTime, qtdTimes)
                    SELECT id, qtdJogadoresPorTime, qtdTimes FROM configuracao_sorteio
                    """
                )

                // 3. Apagar a tabela antiga
                database.execSQL("DROP TABLE configuracao_sorteio")

                // 4. Renomear a tabela temporária
                database.execSQL("ALTER TABLE configuracao_sorteio_temp RENAME TO configuracao_sorteio")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migração da versão 4 para 5
                // Se não houver alterações estruturais necessárias, podemos deixar vazio
                // Esta migração apenas atualiza o número da versão para corresponder ao novo schema
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adiciona as novas colunas à tabela historico_time
                database.execSQL("ALTER TABLE historico_time ADD COLUMN mediaEstrelas REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE historico_time ADD COLUMN mediaPontuacao REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE historico_time ADD COLUMN isUltimoPelada INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Criar a tabela de histórico de peladas
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `historico_pelada` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dataFinalizacao` INTEGER NOT NULL,
                        `times` TEXT NOT NULL
                    )
                    """
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adicionar coluna 'disponivel' à tabela 'jogadores' com valor padrão de 1 (true)
                database.execSQL("ALTER TABLE jogadores ADD COLUMN disponivel INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adicionar a coluna ehTimeReserva à tabela historico_time com valor padrão 0 (false)
                database.execSQL("ALTER TABLE `historico_time` ADD COLUMN `ehTimeReserva` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Criar a tabela grupo_pelada
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `grupo_pelada` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nome` TEXT NOT NULL,
                        `local` TEXT NOT NULL,
                        `horario` TEXT NOT NULL,
                        `dataCriacao` INTEGER NOT NULL,
                        `ultimaModificacao` INTEGER NOT NULL,
                        `imagemUrl` TEXT,
                        `descricao` TEXT,
                        `ativo` INTEGER NOT NULL,
                        `jogadoresIds` TEXT NOT NULL,
                        `usuarioId` TEXT NOT NULL,
                        `compartilhado` INTEGER NOT NULL
                    )
                    """
                )

                // Inserir um grupo padrão para manter compatibilidade com dados existentes
                val dataHora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                database.execSQL(
                    """
                    INSERT INTO `grupo_pelada` (
                        `nome`, `local`, `horario`, `dataCriacao`, `ultimaModificacao`, 
                        `ativo`, `jogadoresIds`, `usuarioId`, `compartilhado`
                    )
                    VALUES (
                        'Pelada Principal', 'Quadra Local', '$dataHora', 
                        ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 
                        1, '[]', 'local', 0
                    )
                    """
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Verificar se a coluna tipoRecorrencia já existe
                val cursor = database.query("PRAGMA table_info(grupo_pelada)")
                val columnNames = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        val columnName = cursor.getString(nameIndex)
                        columnNames.add(columnName)
                    }
                }
                cursor.close()

                // 1. Criar uma tabela temporária com a estrutura correta, usando TEXT para os enums
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `grupo_pelada_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nome` TEXT NOT NULL,
                        `local` TEXT NOT NULL,
                        `horario` TEXT NOT NULL,
                        `dataCriacao` INTEGER NOT NULL,
                        `ultimaModificacao` INTEGER NOT NULL,
                        `imagemUrl` TEXT,
                        `descricao` TEXT,
                        `ativo` INTEGER NOT NULL,
                        `jogadoresIds` TEXT NOT NULL,
                        `usuarioId` TEXT NOT NULL,
                        `compartilhado` INTEGER NOT NULL,
                        `tipoRecorrencia` TEXT NOT NULL DEFAULT 'ESPORADICA',
                        `diaSemana` TEXT,
                        `latitude` REAL,
                        `longitude` REAL,
                        `endereco` TEXT,
                        `localNome` TEXT
                    )
                    """
                )

                // 2. Copiar os dados da tabela antiga para a nova
                if (columnNames.contains("tipoRecorrencia") && columnNames.contains("diaSemana")) {
                    // Se as colunas já existem, fazemos a cópia adequada
                    if (columnNames.contains("tipoRecorrencia") && isColumnType(database, "grupo_pelada", "tipoRecorrencia", "INTEGER")) {
                        // Se tipoRecorrencia já existe como INTEGER, convertemos para TEXT
                        database.execSQL(
                            """
                            INSERT INTO grupo_pelada_temp (
                                id, nome, local, horario, dataCriacao, ultimaModificacao,
                                imagemUrl, descricao, ativo, jogadoresIds, usuarioId, compartilhado,
                                tipoRecorrencia, diaSemana, latitude, longitude, endereco, localNome
                            )
                            SELECT
                                id, nome, local, horario, dataCriacao, ultimaModificacao,
                                imagemUrl, descricao, ativo, jogadoresIds, usuarioId, compartilhado,
                                CASE
                                    WHEN tipoRecorrencia = 0 THEN 'ESPORADICA'
                                    WHEN tipoRecorrencia = 1 THEN 'RECORRENTE'
                                    ELSE 'ESPORADICA'
                                END,
                                CASE
                                    WHEN diaSemana = 0 THEN 'DOMINGO'
                                    WHEN diaSemana = 1 THEN 'SEGUNDA'
                                    WHEN diaSemana = 2 THEN 'TERCA'
                                    WHEN diaSemana = 3 THEN 'QUARTA'
                                    WHEN diaSemana = 4 THEN 'QUINTA'
                                    WHEN diaSemana = 5 THEN 'SEXTA'
                                    WHEN diaSemana = 6 THEN 'SABADO'
                                    ELSE NULL
                                END,
                                latitude, longitude, endereco, localNome
                            FROM grupo_pelada
                            """
                        )
                    } else {
                        // Se tipoRecorrencia já existe como TEXT, apenas copiamos
                        database.execSQL(
                            """
                            INSERT INTO grupo_pelada_temp 
                            SELECT * FROM grupo_pelada
                            """
                        )
                    }
                } else {
                    // Se as colunas não existem, usamos valores padrão
                    database.execSQL(
                        """
                        INSERT INTO grupo_pelada_temp (
                            id, nome, local, horario, dataCriacao, ultimaModificacao,
                            imagemUrl, descricao, ativo, jogadoresIds, usuarioId, compartilhado,
                            tipoRecorrencia, diaSemana, latitude, longitude, endereco, localNome
                        )
                        SELECT
                            id, nome, local, horario, dataCriacao, ultimaModificacao,
                            imagemUrl, descricao, ativo, jogadoresIds, usuarioId, compartilhado,
                            'ESPORADICA', NULL, NULL, NULL, NULL, NULL
                        FROM grupo_pelada
                        """
                    )
                }

                // 3. Apagar a tabela antiga
                database.execSQL("DROP TABLE grupo_pelada")

                // 4. Renomear a tabela temporária
                database.execSQL("ALTER TABLE grupo_pelada_temp RENAME TO grupo_pelada")
            }

            // Função auxiliar para verificar o tipo de uma coluna
            private fun isColumnType(database: SupportSQLiteDatabase, tableName: String, columnName: String, expectedType: String): Boolean {
                val cursor = database.query("PRAGMA table_info($tableName)")
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (name == columnName) {
                            val typeIndex = cursor.getColumnIndex("type")
                            if (typeIndex != -1) {
                                val type = cursor.getString(typeIndex)
                                cursor.close()
                                return type == expectedType
                            }
                        }
                    }
                }
                cursor.close()
                return false
            }
        }

        // Migração para adicionar a coluna diasSemana à tabela grupo_pelada
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adicionar o campo diasSemana à tabela grupo_pelada
                database.execSQL("ALTER TABLE grupo_pelada ADD COLUMN diasSemana TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Criar uma tabela temporária com a nova estrutura incluindo o campo grupoId
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `configuracao_sorteio_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nome` TEXT NOT NULL,
                        `qtdJogadoresPorTime` INTEGER NOT NULL,
                        `qtdTimes` INTEGER NOT NULL,
                        `aleatorio` INTEGER NOT NULL,
                        `criteriosExtras` TEXT NOT NULL,
                        `isPadrao` INTEGER NOT NULL,
                        `grupoId` INTEGER NOT NULL DEFAULT 0
                    )
                    """
                )

                // Copiar os dados da tabela antiga para a nova, com grupoId padrão 0
                database.execSQL(
                    """
                    INSERT INTO configuracao_sorteio_temp (id, nome, qtdJogadoresPorTime, qtdTimes, aleatorio, criteriosExtras, isPadrao, grupoId)
                    SELECT id, nome, qtdJogadoresPorTime, qtdTimes, aleatorio, criteriosExtras, isPadrao, 0 FROM configuracao_sorteio
                    """
                )

                // Apagar a tabela antiga
                database.execSQL("DROP TABLE configuracao_sorteio")

                // Renomear a tabela temporária
                database.execSQL("ALTER TABLE configuracao_sorteio_temp RENAME TO configuracao_sorteio")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Coluna que vincula um grupo local ao documento correspondente na coleção "grupos" do Firestore,
                // quando o grupo é compartilhado com outras pessoas. Null = grupo ainda é só local.
                database.execSQL("ALTER TABLE grupo_pelada ADD COLUMN firestoreId TEXT DEFAULT NULL")
            }
        }
    }
}
