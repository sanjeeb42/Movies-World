package com.movieflix.movieApi.service;

import com.movieflix.movieApi.dto.MovieDto;
import com.movieflix.movieApi.entities.Movie;
import com.movieflix.movieApi.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieServiceImpl implements MovieService{

    private final MovieRepository movieRepository;

    private final FileService fileService;


    public MovieServiceImpl(MovieRepository movieRepository, FileService fileService) {
        this.movieRepository = movieRepository;
        this.fileService = fileService;
    }

    @Value("${project.poster}")
    private String path;

    @Value("${base.url}")
    private String baseUrl;

    @Override
    public MovieDto addMovie(MovieDto movieDto, MultipartFile file) throws IOException {

        if(Files.exists(Paths.get(path + File.separator+file.getOriginalFilename()))){
            throw new RuntimeException("File Already Exists! Please enter another file name");
        }
        //First upload the file, so that we can get a filename
        String uploadedFileName=fileService.uploadFile(path,file);

        //Now set the value of field poster as filename
        movieDto.setPoster(uploadedFileName);

        //Map MovieDto to Movie object since our db only accepts Movie object
        Movie movie=new Movie(
                null,
                movieDto.getTitle(),
                movieDto.getDirector(),
                movieDto.getStudio(),
                movieDto.getMovieCast(),
                movieDto.getReleaseYear(),
                movieDto.getPoster()
        );

        //Save the Movie object
        Movie savedMovie=movieRepository.save(movie);

        //Generate the posterurl and set it
        String posterUrl= baseUrl +"/file/"+uploadedFileName;

        //Map  and return this movieDTO
        MovieDto response =new MovieDto(
                savedMovie.getMovieId(),
                savedMovie.getTitle(),
                savedMovie.getDirector(),
                savedMovie.getStudio(),
                savedMovie.getMovieCast(),
                savedMovie.getReleaseYear(),
                savedMovie.getPoster(),
                posterUrl
        );
        return response;
    }

    @Override
    public MovieDto getMovie(Integer movieId) {

        //1. Check the data in Db if it exists and fetch the data of given ID
        Movie movie= movieRepository.findById(movieId).orElseThrow(()->new RuntimeException("Movie Not Found"));

        //2. Generate Posterurl
        String posterUrl= baseUrl +"/file/"+movie.getPoster();

        //3. Map to movie DTO obejct and return
        MovieDto response=new MovieDto(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getStudio(),
                movie.getMovieCast(),
                movie.getReleaseYear(),
                movie.getPoster(),
                posterUrl
        );
        return response;
    }

    @Override
    public List<MovieDto> getAllMovies() {
        //1. Fetch all the data from db
        List<Movie> movies=movieRepository.findAll();
        //2. Iterate throught the list and generate poster url for each movie object and map to movie Dto object
        List<MovieDto> movieDtos=new ArrayList<>();

        for(Movie movie: movies){
            String posterUrl= baseUrl +"/file/"+movie.getPoster();
            MovieDto response=new MovieDto(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl
            );
            movieDtos.add(response);
        }
        return movieDtos;
    }

    @Override
    public MovieDto updateMovie(Integer movieId, MovieDto movieDto, MultipartFile file) throws IOException {
        // First Fetch the Movie Object from movieId
        return null;
    }

    @Override
    public String deleteMovie(Integer movieId) {
        return "";
    }
}
