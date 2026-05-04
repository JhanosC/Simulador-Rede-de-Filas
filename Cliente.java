public class Cliente {
    private int id;
    private double tempoChegada;
    private double tempoSaida;

    public Cliente(int id, double tempoChegada) {
        this.id = id;
        this.tempoChegada = tempoChegada;
        this.tempoSaida = -1;
    }

    public int getId() {
        return id;
    }

    public double getTempoChegada() {
        return tempoChegada;
    }

    public double getTempoSaida() {
        return tempoSaida;
    }

    public void setTempoSaida(double tempo) {
        this.tempoSaida = tempo;
    }

    @Override
    public String toString() {
        return "Cliente " + id + " (chegada: " + String.format("%.2f", tempoChegada) + ")"
                + "(saída: " + String.format("%.2f", tempoSaida) + ")";
    }
}
