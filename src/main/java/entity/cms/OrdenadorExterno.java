package entity.cms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class OrdenadorExterno {
    private static final int TAMANHO_BLOCO = 10000;

    public static void ordenarArquivo(String nomeArquivo, Comparator<Cliente> comparador) {
        try {
            List<String> blocos = dividirEmBlocosOrdenados(nomeArquivo, comparador);

            mergeBlocos(blocos, nomeArquivo, comparador);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> dividirEmBlocosOrdenados(String nomeArquivo, Comparator<Cliente> comparador) throws IOException {
        List<String> blocos = new ArrayList<>();
        BufferDeClientes bufferLeitura = new BufferDeClientes();
        ArquivoCliente arquivo = new ArquivoCliente();
        bufferLeitura.associaBuffer(arquivo);
        bufferLeitura.inicializaBuffer("leitura", nomeArquivo);

        int contadorBloco = 0;
        List<Cliente> blocoAtual = new ArrayList<>();

        Cliente cliente;
        while ((cliente = bufferLeitura.proximoCliente()) != null) {
            blocoAtual.add(cliente);
            if (blocoAtual.size() >= TAMANHO_BLOCO) {
                blocoAtual.sort(comparador);
                String nomeBloco = "bloco_" + contadorBloco + ".dat";
                salvarBloco(blocoAtual, nomeBloco);
                blocos.add(nomeBloco);
                blocoAtual.clear();
                contadorBloco++;
            }
        }

        if (!blocoAtual.isEmpty()) {
            blocoAtual.sort(comparador);
            String nomeBloco = "bloco_" + contadorBloco + ".dat";
            salvarBloco(blocoAtual, nomeBloco);
            blocos.add(nomeBloco);
        }

        bufferLeitura.fechaBuffer();
        return blocos;
    }

    private static void salvarBloco(List<Cliente> bloco, String nomeBloco) throws IOException {
        ArquivoCliente arquivoBloco = new ArquivoCliente();
        arquivoBloco.abrirArquivo(nomeBloco, "escrita", Cliente.class);
        arquivoBloco.escreveNoArquivo(bloco);
        arquivoBloco.fechaArquivo();
    }

    private static void mergeBlocos(List<String> blocos, String nomeArquivoSaida, Comparator<Cliente> comparador) throws IOException {
        List<BufferDeClientes> buffers = new ArrayList<>();
        ArquivoCliente arquivoSaida = new ArquivoCliente();
        arquivoSaida.abrirArquivo(nomeArquivoSaida, "escrita", Cliente.class);

        for (String bloco : blocos) {
            ArquivoCliente arquivoBloco = new ArquivoCliente();
            BufferDeClientes buffer = new BufferDeClientes();
            buffer.associaBuffer(arquivoBloco);
            buffer.inicializaBuffer("leitura", bloco);
            buffers.add(buffer);
        }

        PriorityQueue<ParClienteBuffer> filaPrioridade = new PriorityQueue<>(
                (a, b) -> comparador.compare(a.cliente, b.cliente)
        );

        for (BufferDeClientes buffer : buffers) {
            Cliente cliente = buffer.proximoCliente();
            if (cliente != null) {
                filaPrioridade.add(new ParClienteBuffer(cliente, buffer));
            }
        }

        List<Cliente> bufferEscrita = new ArrayList<>();
        while (!filaPrioridade.isEmpty()) {
            ParClienteBuffer par = filaPrioridade.poll();
            bufferEscrita.add(par.cliente);

            if (bufferEscrita.size() >= 10000) {
                arquivoSaida.escreveNoArquivo(bufferEscrita);
                bufferEscrita.clear();
            }

            Cliente proximoCliente = par.buffer.proximoCliente();
            if (proximoCliente != null) {
                filaPrioridade.add(new ParClienteBuffer(proximoCliente, par.buffer));
            }
        }

        if (!bufferEscrita.isEmpty()) {
            arquivoSaida.escreveNoArquivo(bufferEscrita);
        }

        for (BufferDeClientes buffer : buffers) {
            buffer.fechaBuffer();
        }
        arquivoSaida.fechaArquivo();

        for (String bloco : blocos) {
            new File(bloco).delete();
        }
    }

    // Classe auxiliar para associar um cliente ao seu buffer de origem
    private static class ParClienteBuffer {
        Cliente cliente;
        BufferDeClientes buffer;

        ParClienteBuffer(Cliente cliente, BufferDeClientes buffer) {
            this.cliente = cliente;
            this.buffer = buffer;
        }
    }
}