import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeradorAleatorios {
    private List<Double> numeros;
    private int indice;
    private Random rand;
    private int limiteRandNum;

    public GeradorAleatorios(List<Double> numerosAleatorios) {
        this.numeros = numerosAleatorios;
        this.indice = 0;
        this.rand = new Random();
        this.limiteRandNum = numerosAleatorios.size();
    }

    public GeradorAleatorios() {
        this.indice = 0;
        this.numeros = new ArrayList<>();
        this.rand = new Random();
        this.limiteRandNum = 0;
    }

    public void gerarNumeros(int numPerSeed) {
        this.limiteRandNum = numPerSeed;
        for (int i = 0; i < numPerSeed; i++) {
            numeros.add(rand.nextDouble());
        }
    }

    public double proximoNumero() {
        if (indice < numeros.size()) {
            return numeros.get(indice++);
        }
        return -1.0;
    }

    public double uniforme(double min, double max) {
        double u = proximoNumero();
        if (u == -1.0)
            return -1.0;
        return min + u * (max - min);
    }

    public String selecionarRota(Map<String, Double> rotasComProb) {
        if (rotasComProb == null || rotasComProb.isEmpty()) {
            return null;
        }

        double u = proximoNumero();
        if (u == -1.0)
            return null;

        double acumulado = 0.0;
        for (Map.Entry<String, Double> entry : rotasComProb.entrySet()) {
            acumulado += entry.getValue();
            if (u < acumulado) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean atingiuLimite() {
        return indice >= numeros.size() || (limiteRandNum > 0 && indice >= limiteRandNum);
    }

    public int getLimit() {
        return limiteRandNum;
    }
}