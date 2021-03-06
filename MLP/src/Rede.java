﻿import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Rede {

	/*
	 * Esta classe controla o funcionamento da Rede Neural MLP.
	 * Se ela for instanciada em modo de treinamento, ela mesma o controlará assim que o treinamento for requisitado.
	 * Por outro lado, se ela for instanciada em modo de execução, ela irá preparar os neurônios com pesos pesos e devolver 
	 * quase imediatamente o controle para a classe Main.
	 */

	
	//************* Formação da instância de rede neural *********************//
	// Vale notar que como os neurônios da primeira camada apenas servem para receber dados, eles não precisam ser instanciados
	
	Neuronio[] camadaEscondida;
	Neuronio[] camadaSaida;

	// MODO DE EXECUÇÃO:
	// Construtor determina quantos neuronios deverão ser criados a partir do comprimento dos arrays de pesos recebidos.
	// Como a camada de entrada não precisa ser instanciada, ela também não tem pesos para guardar.
	// Este construtor será invocado no caso de a rede estar sendo criada em modo de execução.
	public Rede(double[][] camadaEscondida, double[][] camadaSaida, double[] viesEscondida, double[] viesSaida){
		
		/*	Neste contexto, camadaEscondida.length é o número de neurônios na camada escondida.
		 *  Enquanto camadaEscondida[0].length, é o número de pesos que cada um desses neurônios 
		 *  precisa armazenar e também o número de neurônios na camada de entrada.
		 */

		// reserva espaço nos arrays para todos os neurônios que serão criados a seguir
		this.camadaEscondida = new Neuronio[camadaEscondida.length];
		this.camadaSaida = new Neuronio[camadaSaida.length];
		
		// Os dois loops a seguir criam os neurônios da camada escondida e inserem os pesos dados
		for(int p = 0; p < camadaEscondida.length; p++){
			// este primeiro loop tem suas iterações controladas pelo número de neurônios na camada de entrada
			this.camadaEscondida[p] = new Neuronio(camadaEscondida[0].length); // cada neurônio é criado com a informação de quantos neurônios existem na camada de entrada
			this.camadaEscondida[p].setVies(viesEscondida[p]);

			for(int k = 0; k < camadaEscondida[0].length; k++)
				// este segundo loop tem suas iterações controladas pela quantidade de pesos que devem ser configurados
				this.camadaEscondida[p].setPeso(k, camadaEscondida[p][k]);
		}

		// Os dois loops a seguir criam os neurônios da camada de saída
		for(int x = 0; x < camadaSaida.length; x++){
			// este primeiro loop tem suas iterações controladas pelo número de neurônios na camada de saída
			this.camadaSaida[x] = new Neuronio(camadaSaida[0].length); // cada neurônio é criado com a informação de quantos neurônios existem na camada escondida
			this.camadaSaida[x].setVies(viesSaida[x]);

			for(int z = 0; z < camadaSaida[0].length; z++)
				// este segundo loop tem suas iterações controladas pela quantidade de pesos que devem ser configurados
				this.camadaSaida[x].setPeso(z, camadaSaida[x][z]);
		}

	}
	
	// MODO DE TREINAMENTO
	// Esse construtor será invocado se a rede for iniciada em modo de treinamento
	// A definição de pesos será delegada para a classe Neuronio,
	// mas cabe a este construtor decidir se o usuário quer pesos 0 ou aleatórios.
	public Rede(int camadaEntrada, int camadaEscondida, int camadaSaida, boolean random){
		
		// reserva espaço nos arrays para todos os neurônios que serão criados a seguir
		this.camadaEscondida = new Neuronio[camadaEscondida];
		this.camadaSaida = new Neuronio[camadaSaida];
		
		// cria os neurônios escondidos
		for(int j = 0; j < camadaEscondida; j++){
			this.camadaEscondida[j] = new Neuronio(camadaEntrada);
			if(random) this.camadaEscondida[j].reset();
			else this.camadaEscondida[j].zera();
		}
		
		// cria os neurônios de saída
		for(int k = 0; k < camadaSaida; k++){
			this.camadaSaida[k] = new Neuronio(camadaEscondida);
			if(random) this.camadaSaida[k].reset();
			else this.camadaSaida[k].zera();
		}
		
	}


	/* Gerador de hash MD5 para Rede.
	*  Usa os hashs gerados por todos os neurônios para criar um identificador único da Rede atual.
	*/

	public String hashString(){

		double hash = 1;

		// concatena todos os hashs MD5 dos Neurônios desta Rede e gera um novo MD5
		for(int i = 0; i <  camadaEscondida.length; i++)
			hash *= camadaEscondida[i].hash();

		for(int i = 0; i <  camadaSaida.length; i++)
			hash += camadaSaida[i].hash();
		
		return Double.toString(hash);


	}
	
	
	//************* Controle para treinamento *********************//
	// A classe aninhada a seguir é dedicada ao controle de fluxo de treinamento da rede neural MLP	
	
	class Treinamento {

		// Variável que armazena as linhas de dados entregues a esta classe para o treinamento.
		Tupla[] treinamento;
		Tupla[] validacao;
		Tupla[] teste;
		
		// taxa de aprendizado deste treinamento
		double aprendizado;
		
		// constantes usadas para definir quais dados serão usados em testes com o método "erros()"
		private static final int TREINAMENTO = 0;
		private static final int VALIDACAO = 1;
		private static final int TESTE = 2;
		
		// cria uma referência no tempo para ser usada na criação de arquivos de log
		private Date date;
		
		
		// construtor recebe todas as linhas do banco de dados 
		public Treinamento(double[][] treinamento, int[] classeTr,	
					double[][] validacao, int[] classeV,
					double[][] teste, int[] classeTe,
					double aprendizado){
			
			this.treinamento = new Tupla[classeTr.length];
			this.validacao = new Tupla[classeV.length];
			this.teste = new Tupla[classeTe.length];
			
			this.aprendizado = aprendizado;
			
			this.date = new Date();
			
			// cria todas as tuplas de treinamento
			for(int i = 0; i < classeTr.length; i++){
				this.treinamento[i] = new Tupla(treinamento[i],classeTr[i]);
			}
			
			// cria todas as tuplas de validação
			for(int i = 0; i < classeV.length; i++){
				this.validacao[i] = new Tupla(validacao[i],classeV[i]);
			}
			
			// cria todas as tuplas de teste
			for(int i = 0; i < classeTe.length; i++){
				this.teste[i] = new Tupla(teste[i],classeTe[i]);
			}
			
		}
		
		// Quando as tuplas forem contruídas externamente, o contrutor a seguir deverá ser usado.
		public Treinamento(Tupla[] treinamento, Tupla[] validacao, Tupla[] teste, double aprendizado){
			this.treinamento = treinamento;
			this.validacao = validacao;
			this.teste = teste;
			this.aprendizado = aprendizado;
			this.date = new Date();
		}

		
		// a melhor rede que for encontrada em treinamento, será guardada nessa variável
		Rede melhorRede;
		
		// Ponto de entrada para o algoritmo de treinamento.
		// "intervalo" armazena o intervalo de épocas em que a rede passará por validação
		// "fracassos" determina quantos intervalos fracassados serão tolerados antes de o método desistir e retornar a melhor Rede encontrada até o momento
		public Rede executar(int intervalo, int fracassos){
			
			int EpocasExecutadas = 0;
			boolean haMelhora = true; // enquanto houver melhora de desempenho, o treinamento continua
			
			// se prepara para armazenar a a rede de melhor desempenho encontrada até o momento
			double melhorResultado = Double.MAX_VALUE;
			melhorRede = null;
			double atualAcuracia;
			double atualErroQuad;
			int fracassosSeguidos = 0;

			// prepara o controle de Log para treinamento e validação
			Log logTreinamento = new Log();
			Log logValidacao = new Log();
			
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			
			PosProcessamento.getInstance(camadaEscondida.length,aprendizado,date);
			
			logTreinamento.setNomeArquivo("redesTreinamento_"+"_nE"+camadaEscondida.length+"_tA"+aprendizado+"__"+dateFormat.format(this.date));
			logValidacao.setNomeArquivo("redesValidacao_"+"_nE"+camadaEscondida.length+"_tA"+aprendizado+"__"+dateFormat.format(this.date));
			
			
			// while determina quando a melhoria não é mais suficiente para prosseguir o treinamento
			while(haMelhora) {
					
					// Loop das épocas de treinamento
					for(int epoca = 1; epoca <= intervalo; epoca++){
						// Loop das tuplas em cada época
						for(int linhaDeDados = 0; linhaDeDados < treinamento.length; linhaDeDados++){
							sessao(treinamento[linhaDeDados]);
						}
						EpocasExecutadas++;
					} // encerra loop das épocas
					
					atualAcuracia = erros(VALIDACAO);
					atualErroQuad = erroQuadMed(VALIDACAO);
					
					
					// guarda o desempenho em Validação
					logValidacao.addDados(EpocasExecutadas,atualAcuracia,atualErroQuad,aprendizado);
					
					
					// se a rede validada teve o melhor resultado até agora, ela é armazenada
					if(atualAcuracia < melhorResultado) {
						melhorResultado = atualAcuracia;
						melhorRede = Rede.this.clonar();
						fracassosSeguidos = 0;
					}
					else{
						fracassosSeguidos++;
					}
					
					// se ocorreram mais fracassos consecutivos do que o permitido, declare a interrupção de treinamento
					if(fracassosSeguidos >= fracassos)
						haMelhora = false;
					
					// Mostra para o usuario o desempenho em Validação
					System.out.println("Épocas executadas: "+EpocasExecutadas);
					System.out.println("Erros em validação: "+ atualAcuracia);
					System.out.println("Erro quadrado em validação: "+ atualErroQuad);
					
					
					// guarda o desempenho em Treinamento
					atualAcuracia = erros(TREINAMENTO);
					atualErroQuad = erroQuadMed(TREINAMENTO);
					logTreinamento.addDados(EpocasExecutadas,atualAcuracia,atualErroQuad,aprendizado);
					
					System.out.println("Erros em treinamento: "+ atualAcuracia);
					System.out.println("Erro quadrado em treinamento: "+ atualErroQuad);
					System.out.println("Taxa de aprendizado: "+aprendizado);
					System.out.println();
					
					//aprendizado = aprendizado*0.999;
				
			} // encerra o while de treinamento
			
			/* Agora que a melhor rede possível foi encontrada, 
			 * ela deve ser avaliada pelo conjunto de teste.
			 * Esse resultado será guardado em um arquivo de log específico.
			 */
			
			Log logTeste = new Log();
			logTeste.setNomeArquivo("redesTeste_"+"_nE"+camadaEscondida.length+"_tA"+aprendizado+"__"+dateFormat.format(date));
			
			double acuraciaFinal = erros(TESTE, melhorRede);
			double erroQuadFinal = erroQuadMed(TESTE, melhorRede);
			logTeste.addDados(EpocasExecutadas,acuraciaFinal,erroQuadFinal,aprendizado,melhorRede.hashString());
			
			// descarrega os logs em disco
			logValidacao.gravaArquivo();
			logTreinamento.gravaArquivo();
			logTeste.gravaArquivo();
			
			System.out.println("Melhor erro quadrado alcançado em validação: "+melhorResultado);
			System.out.println("Erros em teste: "+acuraciaFinal);
			System.out.println("Erro quadrado em teste: "+erroQuadFinal);
			
			// prepara e salva em disco a matriz de confusão
			int[][] matrizConfusao = matrizConfusao(melhorRede);
			salvaMatriz(matrizConfusao);
			
			// salva em disco a melhorRede
			this.salva(melhorRede);
			
			// envia dados coletados para o pós-processamento
			PosProcessamento.getInstance().addMatriz(matrizConfusao);
			
			double[] temp = {acuraciaFinal,erroQuadFinal};
			PosProcessamento.getInstance().addErro(temp);
			
			
			// retorna a melhor rede neural encontrada no processo de treinamento
			return melhorRede;
		}
		
		// uma sessao de treinamento inclui uma operação de feedforward e backpropagation
		private void sessao(Tupla tupla){
			
			// se prepara para armazenar os resultados da camada escondida e dacamada de saída
			double[] zJ = new double[camadaEscondida.length];
			double[] yK = new double[camadaSaida.length];
			
			// chama feedForward para cada neurônio da camada escondida
			for(int neuronioEsc = 0; neuronioEsc < camadaEscondida.length; neuronioEsc++){
				zJ[neuronioEsc] = camadaEscondida[neuronioEsc].feedForward(tupla);
			}
			
			// chama feedFoward para cada neurônio da camada de saída
			for(int neuronioSai = 0; neuronioSai < camadaSaida.length; neuronioSai++){
				yK[neuronioSai] = camadaSaida[neuronioSai].feedForward(zJ);
			}
			
			
			
			/*
			 * Backpropagation na camada de saída
			 * 
			 * Todos os neurônios devem receber o sinal de treinamento -1,
			 * exceto pelo neurônio que corresponde à classe da tupla dada, 
			 * que deve receber 1.
			 */
			double tk;
			double[] deltaK = new double[camadaSaida.length];
			double[][] delta_wJK = new double[camadaSaida.length][camadaEscondida.length];
			double[] delta_w0K = new double[camadaSaida.length];
			for(int k = 0; k < camadaSaida.length; k++){
				
				if(k == tupla.classe())
					tk = 1;	
				else
					tk = -1;
				
				// armaezena o tK em Neuronio para computar o erroQuadrado após o fim da sessão
				camadaSaida[k].tk = tk;
				
				// executa os primeiros cálculos da backpropagation
				deltaK[k] = (tk - yK[k])*camadaSaida[k].derivada();
				
				for(int j = 0; j < camadaEscondida.length; j++)
					delta_wJK[k][j] = aprendizado*deltaK[k]*zJ[j];
				
				delta_w0K[k] = aprendizado*deltaK[k];
				
			}
			
			/*
			 *  Backpropagation na camada escondida
			 */
			
			// prepara o cálculo para o termo de erro de informação
			double[] delta_inJ = new double[camadaEscondida.length];
			double[] deltaJ = new double[camadaEscondida.length];
			
			double[][] delta_vIJ = new double[camadaEscondida.length][camadaEscondida[0].peso.length];
			double[] delta_v0J = new double[camadaEscondida.length];
			
			for(int j = 0; j < camadaEscondida.length; j++){
				// faz o somatório para cada input de delta
				for(int k = 0; k < camadaSaida.length; k++){
					delta_inJ[j] += deltaK[k]*camadaSaida[k].getPeso(j);
				}
				
				// calcula o termo de erro de informação
				deltaJ[j] = delta_inJ[j]*camadaEscondida[j].derivada();
				
				// calcula a correção para cada peso do neurônio ativo
				for(int i = 0; i < tupla.length(); i++)
					delta_vIJ[j][i] = aprendizado*deltaJ[j]*tupla.valor(i);
				
				delta_v0J[j] = aprendizado*deltaJ[j];
				
			}
			
			// atualiza pesos e viés na camada de saída
			for(int k = 0; k < camadaSaida.length; k++){
				camadaSaida[k].setVies(camadaSaida[k].getVies()+delta_w0K[k]);
				for(int j = 0; j < camadaEscondida.length; j++)
					camadaSaida[k].setPeso(j, delta_wJK[k][j]);
			}
			
			
			// atualiza pesos e viés na camada escondida
			for(int j = 0; j < camadaEscondida.length; j++){

				camadaEscondida[j].setVies(camadaEscondida[j].getVies()+delta_v0J[j]);
				for(int i = 0; i < tupla.length(); i++)
					camadaEscondida[j].setPeso(i, delta_vIJ[j][i]);
			}
			
			
		} // fim de uma sessão de Treinamento
		
		/*
		 * Retorna a quantidade de erros (acurácia) que a rede especificada 
		 * consegue alcançar no conjunto de dados apontado via parâmetro.
		 */
		private double erros(int META, Rede mlp){
			
			// decide qual será o conjunto de dados usado para testar desempenho
			Tupla[] entrada = null;
			if(META == TREINAMENTO)
				entrada = treinamento;
			else if(META == VALIDACAO)
				entrada = validacao;
			else if(META == TESTE)
				entrada = teste;
			
			
			int tentativas = entrada.length;
			int erros = 0;
			int resultado;
			
			
			for(int i = 0; i < tentativas; i++){
				resultado = mlp.executar(entrada[i]);
				if(resultado != entrada[i].classe())
					erros++;
			}
			return (double)erros/tentativas;
		}
		
		/* Quando a rede a ser avaliada não for especificada,
		 * ele assume que deve avaliar a rede da instância atual.
		 */
		private double erros(int META){
			return erros(META,Rede.this);
		}
		
		/*
		 * Encontra o erro quadrado médio para a Rede especificada.
		 */
		private double erroQuadMed(int META, Rede mlp){
			// decide qual será o conjunto de dados usado para testar desempenho
			Tupla[] entrada = null;
			if(META == TREINAMENTO)
				entrada = treinamento;
			else if(META == VALIDACAO)
				entrada = validacao;
			else if(META == TESTE)
				entrada = teste;
			
			double erroQuad = 0;
			double temp = 0;
			
			for (int i = 0; i < entrada.length; i++) {
				mlp.executar(entrada[i]);
				
				// encontra o erro quadrado médio para a tupla executada
				for (int j = 0; j < mlp.camadaSaida.length; j++) {
					temp += Math.pow((mlp.camadaSaida[j].tk - mlp.camadaSaida[j].fAtivacao), 2); 
				}
				erroQuad += temp;
				temp = 0;
				
			}
			return erroQuad;
		}
		
		/* Quando a rede a ser avaliada não for especificada,
		 * ele assume que deve avaliar a rede da instância atual.
		 */
		private double erroQuadMed(int META){
			return erroQuadMed(META,Rede.this);
		}
		
		/* Cria a matriz de confusão da rede neural especificada
		 * via parâmetro. Esse mapeamento sempre será 
		 * feito com o conjunto de teste.
		 * As linhas da matriz gerada representam a realidade, 
		 * enquanto as colunas mostram as respostas da rede.
		 */
		private int[][] matrizConfusao(Rede mlp){
			
			int[][] matriz = new int[10][10];
			
			// roda por todas as linhas de dados em teste
			for(int i=0; i<teste.length; i++){
				matriz[teste[i].classe()][mlp.executar(teste[i])]++;
			}
			
			return matriz;
		}
		
		/* 
		 * Recebe uma matriz de confusão e a salva em arquivo
		 */
		private void salvaMatriz(int[][] matriz) {
			
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			PrintStream pr;

			try {
				pr = new PrintStream(new File("matrizConfusao_"+"_nE"+camadaEscondida.length+"_tA"+aprendizado+"__"+dateFormat.format(date)+".csv"));
				
				// printa um cabeçalho identificando a rede
				pr.print("Rede id: "+melhorRede.hashString());
				pr.println();
				
				pr.print("Linhas representam a realidade, enquanto colunas mostram as respostas da rede.");
				pr.println();
				
				// printa os header de colunas
				pr.print("	");
				for (int i = 0; i < 10; i++) {
					pr.print("	"+i);
				}
				pr.println();

				// printa matriz
				for (int i = 0; i < matriz.length; i++) {
					pr.print("	"+i);
					for (int j = 0; j < matriz[i].length; j++) {
						pr.print("	"+matriz[i][j]);
					}
					pr.println();
				}
				pr.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
		}
		
		// Salva a instância especificada de Rede em disco
		public void salva(Rede mlp){
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			PrintStream pr;

			try {
				pr = new PrintStream(new File("redeFinal_"+"_nE"+camadaEscondida.length+"_tA"+aprendizado+"__"+dateFormat.format(date)+".csv"));
				
				// printa um cabeçalho identificando a rede
				pr.print("Rede id: "+mlp.hashString());
				pr.println();
				
				// printa os header de colunas
				pr.print(mlp.toString());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}



	} // fim da classe aninhada de Treinamento
	
	//************* Controle para execução *********************//
	// classe executar de Rede faz a operação de FeedForward e retorna a classe a que a tupla deve pertencer
	public int executar(Tupla tupla){
		
		// passa todas as colunas da tupla para a camada escondida e armazena os resultados
		double[] z = new double[camadaEscondida.length];
		for(int i = 0; i < camadaEscondida.length; i++)
			z[i] = camadaEscondida[i].feedForward(tupla);
		
		// passa todos os valores da camada escondida para a camanda de saída e armazena os resultados finais
		double[] y = new double[camadaSaida.length];
		for(int i = 0; i < camadaSaida.length; i++)
			y[i] = camadaSaida[i].feedForward(z);
		
		return decide(y);
		
	}
	
	/*
	 * Método recebe um array de doubles gerado pela camada de saída e interpreta a decisão da rede.
	 * O neurônio que responder com o maior valor será considerado o decisor.
	 */
	
	private int decide(double[] saida){
		
		int ret = -1;
		double maior = Double.MAX_VALUE*(-1); // menor double possível
		
		// encontra o neurônio que retornou o maior valor
		for(int i = 0; i < saida.length; i++){
			if(saida[i] > maior){
				maior = saida[i];
				ret = i;
			}
		}
		
		return ret;
		
	}
	
	// Clona uma Rede e retorna a referência ao novo objeto
	public Rede clonar(){
		return Rede.fromString(this.toString());
	}
	
		
	// especifica como salvar a rede em formato String
	public String toString(){
		StringBuffer rede = new StringBuffer();
		rede.append(camadaEscondida.length + ";" + camadaSaida.length + ";" + camadaEscondida[0].peso.length + ";" + camadaSaida[0].peso.length + ";\n");
		//rede.append(viesEscondida + ";" + viesSaida + "\n");
		// Camada escondida
		for(int i = 0; i < camadaEscondida.length; i++) {
			rede.append("1;"); // 1 = camada escondida
			rede.append(camadaEscondida[i].getVies()); // A primeira coluna de cada linha conterá o viés
			for(int j = 0; j < camadaEscondida[i].peso.length; j++) {
				rede.append(";" + camadaEscondida[i].peso[j]);
			}
			rede.append("\n");
		}
		
		// Camada saída
		for(int i = 0; i < camadaSaida.length; i++) {
			rede.append("2;"); // 2 = camada saída
			rede.append(camadaSaida[i].getVies()); // A primeira coluna de cada linha conterá o viés
			for(int j = 0; j < camadaSaida[i].peso.length; j++) {
				rede.append(";" + camadaSaida[i].peso[j]);
			}
			rede.append("\n");
		}
		
		return rede.toString();
	}
	
	// especifica como resgatar uma rede salva em string e retorna um ponteiro para a nova rede
	public static Rede fromString(String s){
		String[] linhas = s.split("\n"); // divide a string por linhas
		
		// A primeira linha da string está no formato "w;x;y;z", onde w = tamanho da camada escondida, x = tamanho da camada de saída, y = tamanho peso camada escondida, z = tamanho peso camada de saída
		String[] tamanhos = linhas[0].split(";"); // separa x e y da primeira linha
		
		// Cria os arranjos com as informações do tamanho das camadas e de seus pesos
		double[][] nCamadaEscondida = new double[Integer.parseInt(tamanhos[0])][Integer.parseInt(tamanhos[2])];
		double[][] nCamadaSaida = new double[Integer.parseInt(tamanhos[1])][Integer.parseInt(tamanhos[3])];
		//System.out.printf("nCamadaEscondida[%s][%s]\nnCamadaSaida[%s][%s]\n", tamanhos[0], tamanhos[2], tamanhos[1], tamanhos[3]);
		
		// As demais linhas representam os neurônios e seus pesos
		int indiceEscondida = 0;
		int indiceSaida = 0;
		int indiceViesEscondida = 0;
		int indiceViesSaida = 0;
		
		double[] viesEscondida = new double[nCamadaEscondida.length];
		double[] viesSaida = new double[nCamadaSaida.length];
		
		//System.out.printf("viesEscondida[%d]\nviesSaida[%d]\n", nCamadaEscondida.length, nCamadaSaida.length);
		
		for(int i = 1; i < linhas.length; i++) { // percorre todas as linhas da string
			String[] neuronios = linhas[i].split(";"); // divide os dados entre os separadores (;)
			if(neuronios[0].equals("1")) { // neurônio de camada escondida
				viesEscondida[indiceViesEscondida] = Double.parseDouble(neuronios[1]);
				indiceViesEscondida++;
				for(int j = 2; j < neuronios.length; j++) {
					nCamadaEscondida[indiceEscondida][j-2] = Double.parseDouble(neuronios[j]);
				}	
				indiceEscondida++; // atualiza o índice da camada escondida
			} else if(neuronios[0].equals("2")) { // neurônio de camada de saída
				viesSaida[indiceViesSaida] = Double.parseDouble(neuronios[1]);
				indiceViesSaida++;
				for(int j = 2; j < neuronios.length; j++) {
					nCamadaSaida[indiceSaida][j-2] = Double.parseDouble(neuronios[j]);
				}
				indiceSaida++; // atualiza o índice da camada de saída
			}
		}
		
		// Invoca o construtor para criar a nova rede
		return new Rede(nCamadaEscondida, nCamadaSaida, viesEscondida, viesSaida);
				
	}


}
