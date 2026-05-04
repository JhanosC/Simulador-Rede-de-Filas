import java.util.*;
import java.io.*;
//import org.yaml.snakeyaml.Yaml;

public class Simulador {
    private double arrival;
    private double minArrivalQ1;
    private double maxArrivalQ1;
    private ArrayList<Fila> listaDefilas;
    private Map<String, Fila> filasPorNome;
    private Map<String, Map<String, Double>> rotas; // origem -> {destino: probabilidade}
    private PriorityQueue<Evento> eventsQueue;
    private GeradorAleatorios rndGenerator;
    private double tempoFinal;
    private int proximoIdCliente;

    public Simulador(String caminhoYAML) {
        arrival = 0;
        minArrivalQ1 = 0;
        maxArrivalQ1 = 0;
        this.listaDefilas = new ArrayList<>();
        this.filasPorNome = new HashMap<>();
        this.rotas = new HashMap<>();
        this.eventsQueue = new PriorityQueue<>();
        this.tempoFinal = 0;
        this.proximoIdCliente = 1;

        carregarModeloYAML(caminhoYAML);
    }

    /**
     * Executa a simulação
     */
    public void execute() {
        System.out.println("\n========== INICIANDO SIMULAÇÃO ==========");
        System.out.println("Limite de números aleatórios: " + rndGenerator.getLimit());
        System.out.println("==========================================\n");

        while (!eventsQueue.isEmpty() && !rndGenerator.atingiuLimite()) {
            Evento evento = eventsQueue.poll();
            tempoFinal = evento.getTempo();
            processarEvento(evento);
        }

        // Finalizar rastreamento de estados
        for (Fila fila : listaDefilas) {
            fila.finalizarRastreamentoEstado(tempoFinal);
        }

        System.out.println("\n========== SIMULAÇÃO FINALIZADA ==========");
        System.out.println("Tempo final: " + String.format("%.2f", tempoFinal));
        imprimirEstatisticas();
    }

    /**
     * Processa um evento
     */
    private void processarEvento(Evento evento) {
        switch (evento.getTipo()) {
            case Evento.CHEGADA:
                procesarChegada(evento);
                break;
            case Evento.SAIDA:
                procesarSaida(evento);
                break;
            case Evento.ROTEAMENTO:
                procesarRoteamento(evento);
                break;
        }
    }

    private void procesarChegada(Evento evento) {
        Cliente cliente = evento.getCliente();
        String nomeFila = evento.getFilaSaida();
        Fila fila = filasPorNome.get(nomeFila);

        fila.registrarMudancaEstado(tempoFinal);

        if (fila.temLugarDisponivel()) {
            fila.adicionarCliente(cliente);

            // System.out.println(
            // "Arriving " + cliente + " on queue: " + nomeFila + " with : "
            // + fila.getQuantidadeClientesNoSistema() + " in queue");

            // Se servidor disponível, iniciar atendimento
            if (fila.temServidorDisponivel()) {
                Cliente emAtendimento = fila.removerCliente();
                fila.iniciarAtendimento();
                double tempoSaida = tempoFinal
                        + rndGenerator.uniforme(fila.getMinTempoAtendimento(), fila.getMaxTempoAtendimento());
                Evento eventoSaida = new Evento(Evento.SAIDA, tempoSaida, emAtendimento, nomeFila);
                eventsQueue.add(eventoSaida);
            }
        } else {
            fila.perderCliente();
        }

        // Gerar próxima chegada em Q1
        if (!rndGenerator.atingiuLimite()) {
            if (nomeFila.equals("Q1") && evento.getTempo() == cliente.getTempoChegada()) {
                double tempoProxChegada = tempoFinal + rndGenerator.uniforme(minArrivalQ1, maxArrivalQ1);
                // System.out.println("Next client in: " + tempoProxChegada);
                Cliente proximoCliente = new Cliente(proximoIdCliente++, tempoProxChegada);
                Evento eventoProx = new Evento(Evento.CHEGADA, tempoProxChegada, proximoCliente, "Q1");
                eventsQueue.add(eventoProx);
            }
        }
        fila.registrarMudancaEstado(tempoFinal);
    }

    private void procesarSaida(Evento evento) {
        Cliente cliente = evento.getCliente();
        String nomeFila = evento.getFilaSaida();
        Fila fila = filasPorNome.get(nomeFila);

        fila.registrarMudancaEstado(tempoFinal);
        cliente.setTempoSaida(tempoFinal);
        fila.terminarAtendimento(tempoFinal - cliente.getTempoChegada());
        // System.out.println("Left " + cliente + " of queue: " + fila.getNome());

        // Verificar se há cliente esperando
        if (fila.temClienteEsperando() && fila.temServidorDisponivel() && !rndGenerator.atingiuLimite()) {
            Cliente proximo = fila.removerCliente();
            fila.iniciarAtendimento();
            double tempoSaida = tempoFinal
                    + rndGenerator.uniforme(fila.getMinTempoAtendimento(), fila.getMaxTempoAtendimento());
            Evento eventoSaida = new Evento(Evento.SAIDA, tempoSaida, proximo, nomeFila);
            eventsQueue.add(eventoSaida);
        }

        // Rotear cliente para próxima fila (ou sair do sistema)
        if (rotas.containsKey(nomeFila) && !rotas.get(nomeFila).isEmpty()) {
            String filaDestino = rndGenerator.selecionarRota(rotas.get(nomeFila));

            if (filaDestino != null) {
                // System.out.println(cliente + " going to: " + filaDestino);
                // Cliente vai para outra fila
                Evento eventoRot = new Evento(Evento.ROTEAMENTO, tempoFinal, cliente, nomeFila);
                eventoRot.setFilaDestino(filaDestino);
                eventsQueue.add(eventoRot);
            }
            // Se filaDestino == null, cliente sai do sistema (não faz nada)
        }
        fila.registrarMudancaEstado(tempoFinal);
        // Se não há rotas definidas para esta fila, cliente sai do sistema
    }

    private void procesarRoteamento(Evento evento) {
        Cliente cliente = evento.getCliente();
        String filaDestino = evento.getFilaDestino();

        if (filaDestino != null && filasPorNome.containsKey(filaDestino)) {
            Fila fila = filasPorNome.get(filaDestino);
            fila.registrarMudancaEstado(tempoFinal);
            // Criar evento de chegada na fila destino
            Evento eventoChegada = new Evento(Evento.CHEGADA, tempoFinal, cliente, filaDestino);
            eventsQueue.add(eventoChegada);
        }
        // Se filaDestino for inválido ou null, cliente sai do sistema (não faz nada)
    }

    // Imprime estatísticas finais

    private void imprimirEstatisticas() {
        System.out.println("\n========== ESTATÍSTICAS ==========");
        for (Fila fila : listaDefilas) {
            fila.imprimirEstatisticas();
        }
        System.out.println("\n========== DISTRIBUIÇÃO DE PROBABILIDADES DE ESTADOS ==========");
        for (Fila fila : listaDefilas) {
            fila.imprimirDistribuicaoEstados();
        }
        System.out.println("=================================\n");
    }

    /**
     * Carrega o modelo de simulação do arquivo YAML (parsing simples)
     */
    private void carregarModeloYAML(String caminhoYAML) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(caminhoYAML));
            String linha;
            String secaoAtual = "";
            int rndnumbersPerSeed = 0;
            ArrayList<Long> seeds = new ArrayList<>();

            while ((linha = reader.readLine()) != null) {
                linha = linha.trim();

                if (linha.startsWith("#") || linha.isEmpty()) {
                    continue;
                }

                if (linha.equals("arrivals:")) {
                    linha = reader.readLine();
                    arrival = Double.parseDouble(linha.replace("Q1:", "").trim());
                }
                if (linha.startsWith("queues:")) {
                    secaoAtual = "queues";
                    continue;
                }
                if (linha.startsWith("network:")) {
                    secaoAtual = "network";
                    continue;
                }
                if (linha.equals("rndnumbers:")) {
                    secaoAtual = "rndnumbers";
                    continue;
                }
                if (linha.startsWith("rndnumbersPerSeed:")) {
                    String[] parts = linha.split(":");
                    if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                        rndnumbersPerSeed = Integer.parseInt(parts[1].trim());
                    }
                    continue;
                }
                if (linha.equals("seeds:")) {
                    secaoAtual = "seeds";
                    continue;
                }

                if (secaoAtual.equals("queues")) {
                    String nomeFila = linha.replace(":", "").trim();
                    int servidores = 0;
                    int capacity = -1;
                    double minTempoAtend = 0.0;
                    double maxTempoAtend = 0.0;
                    double minArrival = 0.0;
                    double maxArrival = 0.0;

                    while ((linha = reader.readLine()) != null) {
                        linha = linha.trim();
                        if (linha.startsWith("servers:")) {
                            servidores = Integer.parseInt(linha.replace("servers:", "").trim());
                        }
                        if (linha.startsWith("capacity:")) {
                            capacity = Integer.parseInt(linha.replace("capacity:", "").trim());
                        }
                        if (linha.startsWith("minService:")) {
                            minTempoAtend = Double.parseDouble(linha.replace("minService:", "").trim());
                        }
                        if (linha.startsWith("maxService:")) {
                            maxTempoAtend = Double.parseDouble(linha.replace("maxService:", "").trim());
                        }
                        if (linha.startsWith("minArrival:")) {
                            minArrival = Double.parseDouble(linha.replace("minArrival:", "").trim());
                        }
                        if (linha.startsWith("maxArrival:")) {
                            maxArrival = Double.parseDouble(linha.replace("maxArrival:", "").trim());
                        }
                        if (linha.startsWith("Q")) { // Começa leitura de nova fila
                            Fila fila = new Fila(nomeFila, servidores, minTempoAtend, maxTempoAtend, capacity);
                            listaDefilas.add(fila);
                            filasPorNome.put(nomeFila, fila);

                            // Se é Q1, guardar os tempos de chegada
                            if (nomeFila.equals("Q1")) {
                                minArrivalQ1 = minArrival;
                                maxArrivalQ1 = maxArrival;
                            }

                            nomeFila = linha.replace(":", "").trim();
                            servidores = 0;
                            capacity = -1;
                            minTempoAtend = 0.0;
                            maxTempoAtend = 0.0;
                            minArrival = 0.0;
                            maxArrival = 0.0;
                        }
                        if (linha.isEmpty())
                            break;
                    }
                    Fila fila = new Fila(nomeFila, servidores, minTempoAtend, maxTempoAtend, capacity);
                    listaDefilas.add(fila);
                    filasPorNome.put(nomeFila, fila);

                    // Se apenas Q1, guardar os tempos de chegada
                    if (nomeFila.equals("Q1")) {
                        minArrivalQ1 = minArrival;
                        maxArrivalQ1 = maxArrival;
                    }

                    secaoAtual = "";

                }
                if (secaoAtual.equals("network")) {
                    String origem = "";
                    String destino = "";
                    double prob = 0;

                    if (linha.contains("source:")) {
                        origem = linha.replace("source:", "").replace("-", "").trim();
                    }

                    while ((linha = reader.readLine()) != null) {
                        linha = linha.trim();

                        if (linha.isEmpty() || linha.startsWith("rndnumbers")) {
                            break;
                        }

                        if (linha.startsWith("-") || linha.startsWith("source:")) {
                            origem = linha.replace("source:", "").replace("-", "").trim();
                        }
                        if (linha.startsWith("target:")) {
                            destino = linha.replace("target:", "").trim();
                        }
                        if (linha.startsWith("probability:")) {
                            prob = Double.parseDouble(linha.replace("probability:", "").trim());

                            // Armazenar rota
                            if (!origem.isEmpty() && !destino.isEmpty()) {
                                if (!rotas.containsKey(origem)) {
                                    rotas.put(origem, new HashMap<>());
                                }
                                rotas.get(origem).put(destino, prob);
                            }
                            origem = "";
                            destino = "";
                            prob = 0;
                        }
                    }
                }

                if (secaoAtual.equals("rndnumbers")) {
                    ArrayList<Double> numeros = new ArrayList<>();
                    while ((linha = reader.readLine()) != null) {
                        if (linha.isEmpty()) {
                            break;
                        }
                        numeros.add(Double.parseDouble(linha.replace("-", "").trim()));
                    }
                    rndGenerator = new GeradorAleatorios(numeros);
                    secaoAtual = "";
                }

                if (secaoAtual.equals("seeds")) {
                    while ((linha = reader.readLine()) != null) {
                        linha = linha.trim();
                        if (linha.isEmpty() || !linha.startsWith("-")) {
                            break;
                        }
                        long seed = Long.parseLong(linha.replace("-", "").trim());
                        seeds.add(seed);
                    }
                    secaoAtual = "";
                }
            }

            reader.close();

            // Se não criou rndGenerator, criar com lista vazia
            if (rndGenerator == null) {
                rndGenerator = new GeradorAleatorios();
                // Se tem seeds, gerar números com aquelas seeds
                if (rndnumbersPerSeed > 0) {
                    rndGenerator.gerarNumeros(rndnumbersPerSeed);
                }
            }

            // Evento inicial: primeira chegada em Q1
            double tempoChegada = arrival;
            Cliente cliente = new Cliente(proximoIdCliente++, tempoChegada);
            Evento evento = new Evento(Evento.CHEGADA, tempoChegada, cliente, "Q1");
            eventsQueue.add(evento);

        } catch (Exception e) {
            System.err.println("Erro ao carregar arquivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String caminhoYAML = "model-t1.yml";
        if (args.length > 0) {
            caminhoYAML = args[0];
        }

        Simulador simulador = new Simulador(caminhoYAML);
        simulador.execute();
    }
}
