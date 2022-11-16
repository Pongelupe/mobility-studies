create table linha_onibus (id_linha int primary key, codigo_linha varchar(15), descricao_linha varchar(70));

create table onibus_tempo_real (codigo_evento int, data_hora timestamp, coord geometry, numero_ordem_veiculo int, velocidade_instantanea int, id_linha int, direcao int, sentido int, distancia_percorrida int);

