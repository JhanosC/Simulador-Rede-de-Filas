/**
 * Classe que representa um evento da simulação
 * Tipos: CHEGADA, SAIDA, ROTEAMENTO
 */
public class Evento implements Comparable<Evento> {
    public static final int CHEGADA = 0;
    public static final int SAIDA = 1;
    public static final int ROTEAMENTO = 2;

    private int tipo;
    private double tempo;
    private Cliente cliente;
    private String filaSaida;
    private String filaDestino;

    public Evento(int tipo, double tempo, Cliente cliente, String fila) {
        this.tipo = tipo;
        this.tempo = tempo;
        this.cliente = cliente;
        this.filaSaida = fila;
        this.filaDestino = null;
    }

    public int getTipo() {
        return tipo;
    }

    public double getTempo() {
        return tempo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getFilaSaida() {
        return filaSaida;
    }

    public String getFilaDestino() {
        return filaDestino;
    }

    public void setFilaDestino(String filaDestino) {
        this.filaDestino = filaDestino;
    }

    @Override
    public int compareTo(Evento outro) {
        return Double.compare(this.tempo, outro.tempo);
    }

    @Override
    public String toString() {
        String tipoStr = tipo == CHEGADA ? "CHEGADA" : tipo == SAIDA ? "SAIDA" : "ROTEAMENTO";
        return "Evento(" + tipoStr + ", t=" + tempo + ", Cliente" + cliente.getId() + ", Fila=" + filaSaida + ")";
    }
}
