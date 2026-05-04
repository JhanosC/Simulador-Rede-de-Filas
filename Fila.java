import java.util.LinkedList;
import java.util.Queue;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe que representa uma fila com um ou mais servidores
 */
public class Fila {
    private String nome;
    private int numServidores;
    private int capacity;
    private Queue<Cliente> filaDeEspera;
    private int clientesEmAtendimento;
    private double minTempoAtendimento; // Tempo mínimo de atendimento
    private double maxTempoAtendimento; // Tempo máximo de atendimento

    // Estatísticas
    private int clientesAtendidos;
    private double tempoTotalEspera;
    private int clientesPerdidos;
    private Map<Integer, Double> tempoEmEstado; // estado (número de clientes) -> tempo acumulado
    private int ultimoEstado;
    private double ultimoTempoMudanca;

    public Fila(String nome, int numServidores, double minTempoAtendimento, double maxTempoAtendimento, int capacity) {
        this.nome = nome;
        this.numServidores = numServidores;
        this.capacity = capacity;
        this.filaDeEspera = new LinkedList<Cliente>();
        this.clientesEmAtendimento = 0;
        this.minTempoAtendimento = minTempoAtendimento;
        this.maxTempoAtendimento = maxTempoAtendimento;
        this.clientesAtendidos = 0;
        this.tempoTotalEspera = 0;
        this.clientesPerdidos = 0;
        this.tempoEmEstado = new HashMap<>();
        this.ultimoEstado = 0;
        this.ultimoTempoMudanca = 0.0;
    }

    public String getNome() {
        return nome;
    }

    public int getNumServidores() {
        return numServidores;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean temClienteEsperando() {
        return !filaDeEspera.isEmpty();
    }

    public int getTamanhoDaFila() {
        return filaDeEspera.size();
    }

    public double getMinTempoAtendimento() {
        return minTempoAtendimento;
    }

    public double getMaxTempoAtendimento() {
        return maxTempoAtendimento;
    }

    public int getClientesAtendidos() {
        return clientesAtendidos;
    }

    public int getClientesPerdidos() {
        return clientesPerdidos;
    }

    public int getClientesEmAtendimento() {
        return clientesEmAtendimento;
    }

    public int getQuantidadeClientesNoSistema() {
        return filaDeEspera.size() + clientesEmAtendimento;
    }

    public double getTempoMedioEspera() {
        if (clientesAtendidos == 0)
            return 0;
        return tempoTotalEspera / clientesAtendidos;
    }

    public void adicionarCliente(Cliente cliente) {
        filaDeEspera.add(cliente);
    }

    public Cliente removerCliente() {
        if (!filaDeEspera.isEmpty()) {
            return filaDeEspera.poll();
        }
        return null;
    }

    public void perderCliente() {
        clientesPerdidos++;
    }

    public boolean temServidorDisponivel() {
        return clientesEmAtendimento < numServidores;
    }

    public boolean temLugarDisponivel() {
        return capacity == -1 ? true : (getQuantidadeClientesNoSistema() < capacity);
    }

    public void iniciarAtendimento() {
        clientesEmAtendimento++;
    }

    public void terminarAtendimento(double tempoEspera) {
        clientesEmAtendimento--;
        clientesAtendidos++;
        tempoTotalEspera += tempoEspera;
    }

    public void imprimirEstatisticas() {
        System.out.println("Fila " + nome + ": " + " clientes atendidos: " + clientesAtendidos +
                " | Tempo médio de espera: " + String.format("%.2f", getTempoMedioEspera()) +
                " | clientes perdidos: " + clientesPerdidos);
    }

    public void registrarMudancaEstado(double tempoAtual) {
        int novoEstado = getQuantidadeClientesNoSistema();

        double tempoNoEstado = tempoAtual - ultimoTempoMudanca;
        if (tempoNoEstado > 0) {
            tempoEmEstado.put(ultimoEstado, tempoEmEstado.getOrDefault(ultimoEstado, 0.0) + tempoNoEstado);
        }

        ultimoEstado = novoEstado;
        ultimoTempoMudanca = tempoAtual;
    }

    public void finalizarRastreamentoEstado(double tempoFinal) {
        double tempoNoEstado = tempoFinal - ultimoTempoMudanca;
        if (tempoNoEstado > 0) {
            tempoEmEstado.put(ultimoEstado, tempoEmEstado.getOrDefault(ultimoEstado, 0.0) + tempoNoEstado);
        }
    }

    public void imprimirDistribuicaoEstados() {
        System.out.print(
                "\nFila " + nome
                        + " G/G/" + numServidores);
        if (capacity > -1) {
            System.out.print("/" + capacity);
        }
        System.out.println(": Distribuição de Probabilidades");

        double tempoTotal = 0;
        for (double tempo : tempoEmEstado.values()) {
            tempoTotal += tempo;
        }

        for (int estado : new java.util.TreeMap<>(tempoEmEstado).keySet()) {
            double tempo = tempoEmEstado.get(estado);
            double prob = tempoTotal > 0 ? tempo / tempoTotal : 0;
            System.out.printf("  %d cliente(s): %.4f (tempo: %.2f)%n", estado, prob, tempo);
        }
    }

    @Override
    public String toString() {
        return "Fila [nome=" + nome + ", numServidores=" + numServidores + ", capacity=" + capacity
                + ", minTempoAtendimento=" + minTempoAtendimento + ", maxTempoAtendimento=" + maxTempoAtendimento + "]";
    }

}
